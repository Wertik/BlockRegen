package nl.aurorion.blockregen.preset.condition;

import nl.aurorion.blockregen.conditional.ConditionContext;
import org.jetbrains.annotations.NotNull;

public interface ContextExtender {
    /**
     * Extend the given ConditionContext by more values. This method is called each time before the condition is called
     * and the resulting context is merged with the original one.
     *
     * @param ctx Original context.
     * @return Newly provided context.
     */
    @NotNull
    ConditionContext extend(@NotNull ConditionContext ctx);
}
