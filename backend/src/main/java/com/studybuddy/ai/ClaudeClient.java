package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls Anthropic's Claude through its official Java SDK. It is the second provider beside
 * {@link GeminiClient}, asked before or after it (see {@link AiProviders}). Each feature has its
 * own instance for its reply size and log name; all of them send through the one SDK client built
 * by {@link ClaudeConfiguration}. It answers the same
 * prompts with the same JSON, so the features' own checks on the reply are unchanged. Any failure is logged without the
 * prompt, file or key and thrown as {@link AiUnavailableException}, exactly as Gemini's are.
 */
public class ClaudeClient implements AiClient {
    private static final Logger LOG = LoggerFactory.getLogger(ClaudeClient.class);
    private static final String PDF = "application/pdf";
    /** Claude sometimes wraps JSON in a Markdown code fence; the features expect the JSON alone. */
    private static final Pattern CODE_FENCE = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL);

    private final ClaudeProperties properties;
    private final String purpose;
    private final long maxTokens;
    private final AiQuota quota;
    private final AnthropicClient client;

    /**
     * @param purpose names the feature in log lines, such as "explanation"; never sent to Claude
     * @param maxTokens the reply size for this feature, which includes Claude's reasoning
     * @param client the shared connection to Claude; empty when Claude is off or has no key
     */
    public ClaudeClient(ClaudeProperties properties, String purpose, long maxTokens, AiQuota quota,
            Optional<AnthropicClient> client) {
        this.properties = properties;
        this.purpose = purpose;
        this.maxTokens = maxTokens;
        this.quota = quota;
        this.client = client.orElse(null);
    }

    @Override
    public String generateJson(Long studentId, String prompt, List<AiAttachment> attachments) {
        // No log line: Claude is optional, so being switched off is the normal case, not a fault.
        if (client == null) {
            throw new AiUnavailableException();
        }
        if (!quota.tryAcquire(studentId)) {
            LOG.warn("Claude {} call refused: over the daily allowance or pausing after a rate limit", purpose);
            throw new AiUnavailableException();
        }
        try {
            Message reply = client.messages().create(requestFor(prompt, attachments));
            String text = textOf(reply).orElseThrow(AiUnavailableException::new);
            LOG.info("Claude answered {}", purpose);
            return text;
        } catch (RateLimitException rateLimited) {
            quota.pauseAfterRateLimit();
            LOG.warn("Claude {} rate limited; pausing calls", purpose);
            throw new AiUnavailableException();
        } catch (AnthropicException failure) {
            LOG.warn("Claude {} unavailable: {}", purpose, failure.getClass().getSimpleName());
            throw new AiUnavailableException();
        }
    }

    /**
     * Files go before the instructions, as Anthropic recommends for prompts about a document. No
     * thinking setting is sent: current models think adaptively, and effort sets how much.
     */
    private MessageCreateParams requestFor(String prompt, List<AiAttachment> attachments) {
        List<ContentBlockParam> parts = new ArrayList<>();
        for (AiAttachment attachment : attachments) {
            if (!PDF.equals(attachment.mediaType())) {
                LOG.warn("Claude {} cannot read a {} attachment", purpose, attachment.mediaType());
                throw new AiUnavailableException();
            }
            String data = Base64.getEncoder().encodeToString(attachment.data());
            parts.add(ContentBlockParam.ofDocument(Base64PdfSource.builder().data(data).build()));
        }
        parts.add(ContentBlockParam.ofText(prompt));
        return MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(maxTokens)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(properties.effort())).build())
                .addUserMessageOfContentBlockParams(parts)
                .build();
    }

    /**
     * The reply's text, only if Claude finished normally. A refusal or a reply cut off at the token
     * limit is unusable, because the JSON may be missing or incomplete.
     */
    private Optional<String> textOf(Message reply) {
        StopReason stopReason = reply.stopReason().orElse(null);
        if (!StopReason.END_TURN.equals(stopReason)) {
            LOG.warn("Claude {} reply unusable: stop reason {}", purpose, stopReason == null ? "missing" : stopReason.asString());
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : reply.content()) {
            block.text().ifPresent(part -> text.append(part.text()));
        }
        String answer = text.toString().strip();
        Matcher fenced = CODE_FENCE.matcher(answer);
        if (fenced.matches()) {
            answer = fenced.group(1);
        }
        return answer.isEmpty() ? Optional.empty() : Optional.of(answer);
    }
}
