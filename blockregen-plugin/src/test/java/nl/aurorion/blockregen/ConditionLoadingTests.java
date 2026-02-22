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

    @BeforeAll
    public static void before() {
        java.util.logging.ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINER);
        logger.setLevel(Level.FINER);
        logger.addHandler(handler);
    }

    private final GenericConditionProvider conditionProvider = GenericConditionProvider.empty();

    public ConditionLoadingTests() {
        this.conditionProvider.addProvider("above", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar("value") > Integer.parseInt(String.valueOf(node)), "above"));
        this.conditionProvider.addProvider("below", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar("value") < Integer.parseInt(String.valueOf(node)), "below"));
        this.conditionProvider.addProvider("equals", (key, node) -> Condition.of((ctx) -> (int) ctx.mustVar("value") == Integer.parseInt(String.valueOf(node)), "equals"));
    }

    @Test
    public void loadsSingleCondition() {
        String input = "conditions:\n  - above: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("above", condition.alias());

        Context ctx = Context.of("value", 3);
        assertTrue(condition.matches(ctx));
        ctx = Context.of("value", 1);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsNegatedConditions() {
        // <= 2
        String input = "conditions:\n  - ^above: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("!above", condition.alias());

        Context ctx = Context.of("value", 1);
        assertTrue(condition.matches(ctx));
        ctx = Context.of("value", 4);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsNegatedConditionsInRelations() {
        // <2; 5)
        String input = "conditions:\n  - ^below: 2\n  - below: 5";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(!below and below)", condition.alias());

        assertFalse(condition.matches(Context.of("value", 1)));
        assertTrue(condition.matches(Context.of("value", 3)));
        assertFalse(condition.matches(Context.of("value", 6)));
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

        Context ctx = Context.of("value", 4);
        assertFalse(condition.matches(ctx));

        ctx = Context.of("value", 5);
        assertFalse(condition.matches(ctx));

        ctx = Context.of("value", 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsMultipleConditionsInOrRelation() {
        String input = "conditions:\n  - below: 2\n  - above: 10";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.OR, conditionProvider);

        assertEquals("(below or above)", condition.alias());

        Context ctx = Context.of("value", 1);
        assertTrue(condition.matches(ctx));

        ctx = Context.of("value", 5);
        assertFalse(condition.matches(ctx));

        ctx = Context.of("value", 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsAnyStackedConditions() {
        // x < 5 && (x < 2 || x == 3)
        String input = "conditions:\n  - below: 5\n  - any:\n    - below: 2\n    - equals: 3";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(below and (below or equals))", condition.alias());

        Context ctx = Context.of("value", 1);
        assertTrue(condition.matches(ctx));

        ctx = Context.of("value", 3);
        assertTrue(condition.matches(ctx));

        ctx = Context.of("value", 4);
        assertFalse(condition.matches(ctx));

        ctx = Context.of("value", 6);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsMapConditions() {
        // (4; 10)
        String input = "conditions:\n  above: 4\n  below: 10";

        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromNodeMultiple(Objects.requireNonNull(conf.get("conditions")), ConditionRelation.AND, conditionProvider);

        assertEquals("(above and below)", condition.alias());

        assertFalse(condition.matches(Context.of("value", 1)));
        assertTrue(condition.matches(Context.of("value", 6)));
        assertFalse(condition.matches(Context.of("value", 16)));
    }

    @Test()
    public void loadsStackedConditionProviders() {
        // (4; 10)
        String input = "conditions:\n  - sqrt:\n    - above: 2\n  - below: 10";

        ConditionProvider sqrtProvider = GenericConditionProvider.empty()
                // The square root of a number has to be above X
                .addProvider("above", (key, node) -> {
                    return Condition.of((ctx) -> {
                        return (double) ctx.mustVar("sqrt") > (int) node;
                    });
                })
                .extender((ctx) -> Context.of("sqrt", Math.sqrt((int) ctx.mustVar("value"))));

        ConditionProvider baseProvider = GenericConditionProvider.empty()
                .addProvider("below", (key, node) -> {
                    return Condition.of((ctx) -> {
                        return (int) ctx.mustVar("value") < (int) node;
                    });
                })
                .addProvider("sqrt", sqrtProvider);

        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, baseProvider);

        assertEquals("(above and below)", condition.alias());

        assertFalse(condition.matches(Context.of("value", 1)));
        assertTrue(condition.matches(Context.of("value", 6)));
        assertFalse(condition.matches(Context.of("value", 16)));
    }

    @Test
    public void loadsConditionsFromMap() {
        // interval (2;5)
        String input = "below: 5\nabove: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromMap(Objects.requireNonNull(conf.getValues(false)), ConditionRelation.AND, conditionProvider);

        assertFalse(condition.matches(Context.of("value", 1)));
        assertTrue(condition.matches(Context.of("value", 3)));
        assertFalse(condition.matches(Context.of("value", 6)));
    }

    @Test
    public void propagatesContextThroughWrappers() {
        Condition condition = Conditions.wrap(
                Condition.of((ctx) -> (double) ctx.mustVar("sqrt") > 2, "sqrt > 2"),
                (ctx) -> {
                    // Take value and sqrt it
                    int v = (int) ctx.mustVar("value");
                    return Context.of("sqrt", Math.sqrt(v));
                }
        );

        assertEquals("sqrt > 2", condition.alias());
        assertTrue(condition.matches(Context.of("value", 9)));
    }

    @Test
    public void mergesContexts() {
        Context context = Context.of("value", 1);
        Context context1 = Context.of("random", 2);

        Context merged = Conditions.mergeContexts(context, context1);

        assertEquals(1, merged.mustVar("value"));
        assertEquals(2, merged.mustVar("random"));
    }

    @Test
    public void mergesContextsOverridesValues() {
        Context context = Context.of("value", 1);
        Context context1 = Context.of("value", 2);

        Context merged = Conditions.mergeContexts(context, context1);

        assertEquals(2, merged.mustVar("value"));
    }
}
