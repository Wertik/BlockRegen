package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;

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
    protected boolean match(ConditionContext original) {
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
    public String toString() {
        return this.composed.toString();
    }
}
