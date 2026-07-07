package nl.aurorion.blockregen;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.preset.condition.ConditionProvider;
import nl.aurorion.blockregen.preset.condition.ConditionRelation;
import nl.aurorion.blockregen.preset.condition.Conditions;
import nl.aurorion.blockregen.preset.condition.GenericConditionProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Log
public class ConditionLoadingTests {

    private static final Logger logger = Logger.getLogger("nl.aurorion.blockregen");

    public static final ContextKey VALUE_KEY = BaseContextKey.of("value");
    public static final ContextKey SQRT_KEY = BaseContextKey.of("sqrt");
    public static final ContextKey RANDOM_KEY = BaseContextKey.of("random");

    @BeforeAll
    public static void before() {
        java.util.logging.ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINER);
        logger.setLevel(Level.FINER);
        logger.addHandler(handler);
    }

    private final GenericConditionProvider conditionProvider = GenericConditionProvider.empty();

    public ConditionLoadingTests() {
        this.conditionProvider.addProvider("above", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar(VALUE_KEY) > Integer.parseInt(String.valueOf(node)), "above"));
        this.conditionProvider.addProvider("below", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar(VALUE_KEY) < Integer.parseInt(String.valueOf(node)), "below"));
        this.conditionProvider.addProvider("equals", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar(VALUE_KEY) == Integer.parseInt(String.valueOf(node)), "equals"));
    }

    @Test
    public void loadsSingleCondition() {
        String input = "conditions:\n  - above: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("above", condition.alias());

        Context ctx = Context.of(VALUE_KEY, 3);
        assertTrue(condition.matches(ctx));
        ctx = Context.of(VALUE_KEY, 1);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsNegatedConditions() {
        // <= 2
        String input = "conditions:\n  - ^above: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("!above", condition.alias());

        Context ctx = Context.of(VALUE_KEY, 1);
        assertTrue(condition.matches(ctx));
        ctx = Context.of(VALUE_KEY, 4);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsNegatedConditionsInRelations() {
        // <2; 5)
        String input = "conditions:\n  - ^below: 2\n  - below: 5";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(!below and below)", condition.alias());

        assertFalse(condition.matches(Context.of(VALUE_KEY, 1)));
        assertTrue(condition.matches(Context.of(VALUE_KEY, 3)));
        assertFalse(condition.matches(Context.of(VALUE_KEY, 6)));
    }

    @Test
    public void throwsOnInvalidCondition() {
        String input = "conditions:\n  - invalid: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        assertThrows(ParseException.class, () -> Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider));
    }

    @Test
    public void loadsMultipleConditionsInAndRelation() {
        String input = "conditions:\n  - above: 2\n  - above: 10";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(above and above)", condition.alias());

        Context ctx = Context.of(VALUE_KEY, 4);
        assertFalse(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 5);
        assertFalse(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsMultipleConditionsInOrRelation() {
        String input = "conditions:\n  - below: 2\n  - above: 10";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.OR, conditionProvider);

        assertEquals("(below or above)", condition.alias());

        Context ctx = Context.of(VALUE_KEY, 1);
        assertTrue(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 5);
        assertFalse(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsAnyStackedConditions() {
        // x < 5 && (x < 2 || x == 3)
        String input = "conditions:\n  - below: 5\n  - any:\n    - below: 2\n    - equals: 3";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(below and (below or equals))", condition.alias());

        Context ctx = Context.of(VALUE_KEY, 1);
        assertTrue(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 3);
        assertTrue(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 4);
        assertFalse(condition.matches(ctx));

        ctx = Context.of(VALUE_KEY, 6);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsMapConditions() {
        // (4; 10)
        String input = "conditions:\n  above: 4\n  below: 10";

        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromNodeMultiple(Objects.requireNonNull(conf.get("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(above and below)", condition.alias());

        assertFalse(condition.matches(Context.of(VALUE_KEY, 1)));
        assertTrue(condition.matches(Context.of(VALUE_KEY, 6)));
        assertFalse(condition.matches(Context.of(VALUE_KEY, 16)));
    }

    @Test()
    public void loadsStackedConditionProviders() {
        // (4; 10)
        String input = "conditions:\n  - sqrt:\n    - above: 2\n  - below: 10";

        ConditionProvider sqrtProvider = GenericConditionProvider.empty()
                // The square root of a number has to be above X
                .addProvider("above", (key, node) -> {
                    return Condition.of((ctx) -> {
                        return (double) ctx.mustVar(SQRT_KEY) > (int) node;
                    });
                })
                .extender((ctx) -> Context.of(SQRT_KEY, Math.sqrt((int) ctx.mustVar(VALUE_KEY))));

        ConditionProvider baseProvider = GenericConditionProvider.empty()
                .addProvider("below", (key, node) -> {
                    return Condition.of((ctx) -> {
                        return (int) ctx.mustVar(VALUE_KEY) < (int) node;
                    });
                })
                .addProvider("sqrt", sqrtProvider);

        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, baseProvider);

        assertEquals("(above and below)", condition.alias());

        assertFalse(condition.matches(Context.of(VALUE_KEY, 1)));
        assertTrue(condition.matches(Context.of(VALUE_KEY, 6)));
        assertFalse(condition.matches(Context.of(VALUE_KEY, 16)));
    }

    @Test
    public void loadsConditionsFromMap() {
        // interval (2;5)
        String input = "below: 5\nabove: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromMap(Objects.requireNonNull(conf.getValues(false)), ConditionRelation.AND, conditionProvider);

        assertFalse(condition.matches(Context.of(VALUE_KEY, 1)));
        assertTrue(condition.matches(Context.of(VALUE_KEY, 3)));
        assertFalse(condition.matches(Context.of(VALUE_KEY, 6)));
    }

    @Test
    public void propagatesContextThroughWrappers() {
        Condition condition = Conditions.wrap(
                Condition.of((ctx) -> (double) ctx.mustVar(SQRT_KEY) > 2, "sqrt > 2"),
                (ctx) -> {
                    // Take value and sqrt it
                    int v = (int) ctx.mustVar(VALUE_KEY);
                    return Context.of(SQRT_KEY, Math.sqrt(v));
                }
        );

        assertEquals("sqrt > 2", condition.alias());
        assertTrue(condition.matches(Context.of(VALUE_KEY, 9)));
    }

    @Test
    public void mergesContexts() {
        Context context = Context.of(VALUE_KEY, 1);
        Context context1 = Context.of(RANDOM_KEY, 2);

        Context merged = Conditions.mergeContexts(context, context1);

        assertEquals(1, merged.mustVar(VALUE_KEY));
        assertEquals(2, merged.mustVar(RANDOM_KEY));
    }

    @Test
    public void mergesContextsOverridesValues() {
        Context context = Context.of(VALUE_KEY, 1);
        Context context1 = Context.of(VALUE_KEY, 2);

        Context merged = Conditions.mergeContexts(context, context1);

        assertEquals(2, merged.mustVar(VALUE_KEY));
    }
}
