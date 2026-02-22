package nl.aurorion.blockregen;

import nl.aurorion.blockregen.conditional.Condition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionTests {

    private static final Context EMPTY_CONTEXT = Context.empty();

    @Test
    public void evaluatesSingleValueExpressions() {
        Condition c1 = Condition.truthy();
        assertTrue(c1.matches(EMPTY_CONTEXT));

        Condition c2 = Condition.falsy();
        assertFalse(c2.matches(EMPTY_CONTEXT));

        Condition c3 = Condition.constant(false);
        assertFalse(c3.matches(EMPTY_CONTEXT));
    }

    @Test
    public void evaluatesSingleLayerStackedExpressions() {
        // true and (false or false)
        Condition c1 = Condition.truthy().and(Condition.falsy().or(Condition.falsy()));
        assertFalse(c1.matches(EMPTY_CONTEXT));

        // true or (false and false)
        Condition c2 = Condition.truthy().or(Condition.falsy().and(Condition.falsy()));
        assertTrue(c2.matches(EMPTY_CONTEXT));
    }

    @Test
    public void evaluatesBasedOnContextValues() {
        Context context = Context.of("value", 10);
        Condition c1 = Condition.of((ctx) -> {
            return (int) ctx.mustVar("value") > 5;
        });
        assertTrue(c1.matches(context));
    }

    @Test
    public void preservesContext() {
        Condition condition = Condition.of((ctx) -> {
            ctx.set("from_condition", true);
            return true;
        });

        Context context = Context.of("hello", "world");

        assertTrue(condition.matches(context));
        assertTrue(context.get("from_condition", Boolean.class));
    }

    @Test
    public void composedConditionsWork() {
        // true and true
        Condition composed = Condition.truthy().and(Condition.truthy());

        Context context = Context.empty();

        assertTrue(composed.matches(context));

        composed = Condition.falsy().and(Condition.truthy());

        assertFalse(composed.matches(context));
    }

    @Test
    public void generatesCorrectAliases() {
        Condition c = Condition.truthy().and(Condition.truthy());
        assertEquals("(true and true)", c.alias());

        Condition aliased = Condition.constant(true, "AlwaysTrue").and(Condition.truthy());
        assertEquals("(AlwaysTrue and true)", aliased.alias());

        Condition stacked = Condition.truthy().and(
                Condition.constant(true, "InnerTruth").or(Condition.falsy())
        );
        assertEquals("(true and (InnerTruth or false))", stacked.alias());
    }
}
