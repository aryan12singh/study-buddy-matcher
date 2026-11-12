package com.studybuddy.ai;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when {@code app.ai.claude.*} switches Claude on and gives it a key. It reads the same
 * settings into {@link ClaudeProperties} and asks {@link ClaudeProperties#ready()}, so there is one
 * definition of "Claude is ready".
 */
class ClaudeReadyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Binder.get(context.getEnvironment()).bind("app.ai.claude", ClaudeProperties.class)
                .map(ClaudeProperties::ready).orElse(false);
    }
}
