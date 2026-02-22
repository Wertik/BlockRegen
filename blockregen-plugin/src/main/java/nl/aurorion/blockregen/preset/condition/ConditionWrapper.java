package nl.aurorion.blockregen.preset.condition;


import lombok.extern.java.Log;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.Context;
import org.jetbrains.annotations.NotNull;

/**
 * Wrap around a condition to provide extra context using a {@link ContextExtender} before calling it.
 */
@Log
public class ConditionWrapper extends Condition {
    private final Condition composed;
    private final ContextExtender extender;

    ConditionWrapper(Condition composed, ContextExtender extender) {
        this.composed = composed;
        this.extender = extender;
    }

    private Context extend(Context original) {
        if (this.extender == null) {
            return original;
        }

        Context result = original;

        try {
            Context additional = this.extender.extend(original);

            // Just in case somebody returns the original.
            if (additional != result) {
                result = Conditions.mergeContexts(additional, original);
            }
            return result;
        } catch (Exception e) {
            log.severe(String.format("Failed to run extender for condition: %s", e.getMessage()));
        }

        return result;
    }

    @Override
    public boolean match(Context original) {
        Context context = this.extend(original);
        return this.composed.matches(context);
    }

    @Override
    @NotNull
    public String alias() {
        return this.composed.alias();
    }

    @Override
    @NotNull
    public String pretty() {
        return this.composed.pretty();
    }

    @Override
    public String toString() {
        return this.composed.toString();
    }
}
