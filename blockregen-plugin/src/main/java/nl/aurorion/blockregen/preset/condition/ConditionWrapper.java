package nl.aurorion.blockregen.preset.condition;


import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.conditional.ConditionContext;
import org.jetbrains.annotations.NotNull;

/**
 * Wrap around a condition to provide extra context using a {@link ContextExtender} before calling it.
 */
public class ConditionWrapper extends Condition {
    private final Condition composed;
    private final ContextExtender extender;

    ConditionWrapper(Condition composed, ContextExtender extender) {
        this.composed = composed;
        this.extender = extender;
    }

    @Override
    public boolean match(ConditionContext original) {
        ConditionContext result = original;
        if (this.extender != null) {
            ConditionContext additional = this.extender.extend(original);

            // Just in case somebody returns the original.
            if (additional != result) {
                result = Conditions.mergeContexts(additional, original);
            }
        }
        return this.composed.matches(result);
    }

    @Override
    @NotNull
    public String alias() {
        return this.composed.alias();
    }

    @Override
    public String toString() {
        return this.composed.toString();
    }
}
