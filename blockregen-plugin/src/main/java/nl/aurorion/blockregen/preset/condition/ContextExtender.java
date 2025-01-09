package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.ConditionContext;
import org.jetbrains.annotations.NotNull;

// Provide additional context on top of what's provided.
public interface ContextExtender {
    @NotNull
    ConditionContext extend(@NotNull ConditionContext ctx);
}
