package com.studybuddy.assistant;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantServiceTest {
    private static final AssistantProperties PROPERTIES = new AssistantProperties(20, 3, "Asia/Singapore", "lite", 400, java.time.Duration.ofSeconds(20), 200, 30);
    private final IntentParser parser = mock(IntentParser.class);

    /** One stand-in intent per type, replying with its own type's name. */
    private static List<AssistantIntent> everyIntent() {
        return Arrays.stream(AssistantIntentType.values()).map(type -> (AssistantIntent) new AssistantIntent() {
            public AssistantIntentType type() { return type; }
            public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
                return AssistantReplyDto.text(type, type.name(), null);
            }
        }).toList();
    }

    private static AssistantRequest asking(AssistantIntentType type) {
        return new AssistantRequest(type, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void handsTheMessageToTheIntentTheAiRecognised() {
        var service = new AssistantService(everyIntent(), parser, mock(AccountAccess.class), PROPERTIES);
        when(parser.parse(7L, "quiz me")).thenReturn(asking(AssistantIntentType.QUIZ_ME));

        assertEquals("QUIZ_ME", service.reply(7L, new AssistantMessageInput("  quiz me  ")).text());
    }

    @Test
    void refusesALongMessageBeforeSpendingAnAiCall() {
        var service = new AssistantService(everyIntent(), parser, mock(AccountAccess.class), PROPERTIES);

        assertThrows(InvalidInputException.class, () -> service.reply(7L, new AssistantMessageInput("x".repeat(21))));
        verify(parser, never()).parse(anyLong(), anyString());
    }

    @Test
    void refusesToStartWithoutAnIntentForEveryType() {
        List<AssistantIntent> missingOne = everyIntent().subList(1, AssistantIntentType.values().length);

        assertThrows(IllegalStateException.class, () -> new AssistantService(missingOne, parser, mock(AccountAccess.class), PROPERTIES));
    }
}
