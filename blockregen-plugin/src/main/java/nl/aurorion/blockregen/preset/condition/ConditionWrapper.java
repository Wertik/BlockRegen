package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;

// A wrapper around another condition to provider extra context.
public class ConditionWrapper extends Condition {
    private final Condition composed;

    private final ContextExtender extender;

    ConditionWrapper(Condition composed, ContextExtender extender) {
        this.composed = composed;
        this.extender = extender;
    }

    @Override
    protected boolean match(ConditionContext originalContext) {
        ConditionContext ctx = originalContext;
        if (this.extender != null) {
            ctx = Conditions.mergeContexts(this.extender.extend(originalContext), originalContext);
        }
        return this.composed.matches(ctx);
    }

    @Override
    public String toString() {
        return this.composed.toString();
    }
}
