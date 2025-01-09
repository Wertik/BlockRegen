package nl.aurorion.blockregen;

import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.configuration.ParseException;
import nl.aurorion.blockregen.preset.condition.ConditionRelation;
import nl.aurorion.blockregen.preset.condition.ConditionServiceProvider;
import nl.aurorion.blockregen.preset.condition.Conditions;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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

    private final ConditionServiceProvider conditionServiceProvider = new ConditionServiceProvider();

    public ConditionLoadingTests() {
        this.conditionServiceProvider.register("above", (node, key) -> Condition.of((ctx) -> (int) ctx.mustVar("value") > Integer.parseInt(String.valueOf(node))).alias("above"));
        this.conditionServiceProvider.register("below", (node, key) -> Condition.of((ctx) -> (int) ctx.mustVar("value") < Integer.parseInt(String.valueOf(node))).alias("below"));
        this.conditionServiceProvider.register("equals", (node, key) -> Condition.of((ctx) -> (int) ctx.mustVar("value") == Integer.parseInt(String.valueOf(node))).alias("equals"));
    }

    @Test
    public void loadsSingleCondition() {
        String input = "conditions:\n  - above: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionServiceProvider::load);

        assertEquals("(TrueCondition and (TrueCondition and above))", condition.toString());

        ConditionContext ctx = ConditionContext.of("value", 3);
        assertTrue(condition.matches(ctx));
        ctx = ConditionContext.of("value", 1);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void throwsOnInvalidCondition() {
        String input = "conditions:\n  - invalid: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        assertThrows(ParseException.class, () -> Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionServiceProvider::load));
    }

    @Test
    public void loadsMultipleConditionsInAndRelation() {
        String input = "conditions:\n  - above: 2\n  - above: 10";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionServiceProvider::load);

        assertEquals("(TrueCondition and (TrueCondition and above) and (TrueCondition and above))", condition.toString());

        ConditionContext ctx = ConditionContext.of("value", 4);
        assertFalse(condition.matches(ctx));

        ctx = ConditionContext.of("value", 5);
        assertFalse(condition.matches(ctx));

        ctx = ConditionContext.of("value", 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsMultipleConditionsInOrRelation() {
        String input = "conditions:\n  - below: 2\n  - above: 10";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.OR, conditionServiceProvider::load);

        assertEquals("(FalseCondition or (TrueCondition and below) or (TrueCondition and above))", condition.toString());

        ConditionContext ctx = ConditionContext.of("value", 1);
        assertTrue(condition.matches(ctx));

        ctx = ConditionContext.of("value", 5);
        assertFalse(condition.matches(ctx));

        ctx = ConditionContext.of("value", 15);
        assertTrue(condition.matches(ctx));
    }

    @Test
    public void loadsAnyStackedConditions() {
        // x < 5 && (x < 2 || x == 3)
        String input = "conditions:\n  - below: 5\n  - any:\n    - below: 2\n    - equals: 3";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionServiceProvider::load);

        assertEquals("(TrueCondition and (TrueCondition and below) and (TrueCondition and (FalseCondition or (TrueCondition and below) or (TrueCondition and equals))))", condition.toString());

        ConditionContext ctx = ConditionContext.of("value", 1);
        assertTrue(condition.matches(ctx));

        ctx = ConditionContext.of("value", 3);
        assertTrue(condition.matches(ctx));

        ctx = ConditionContext.of("value", 4);
        assertFalse(condition.matches(ctx));

        ctx = ConditionContext.of("value", 6);
        assertFalse(condition.matches(ctx));
    }

    @Test()
    @Disabled
    public void defaultConditionProviderCompilesProperly() {
        // x < 5 && (x < 2 || x == 3)
        String input = "conditions:\n  - any:\n    - has_extra:\n      extra: 2\n    - below: 5";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromList(Objects.requireNonNull(conf.getList("conditions")), ConditionRelation.AND, conditionServiceProvider::load);

        assertEquals("(TrueCondition and (TrueCondition and below) and (TrueCondition and (FalseCondition or (TrueCondition and below) or (TrueCondition and equals))))", condition.toString());

        ConditionContext ctx = ConditionContext.of("value", 1);
        assertTrue(condition.matches(ctx));

        ctx = ConditionContext.of("value", 3);
        assertTrue(condition.matches(ctx));

        ctx = ConditionContext.of("value", 4);
        assertFalse(condition.matches(ctx));

        ctx = ConditionContext.of("value", 6);
        assertFalse(condition.matches(ctx));
    }

    @Test
    public void loadsConditionsFromMap() {
        // interval (2;5)
        String input = "below: 5\nabove: 2";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));
        Condition condition = Conditions.fromMap(Objects.requireNonNull(conf.getValues(false)), ConditionRelation.AND, conditionServiceProvider::load);

        assertFalse(condition.matches(ConditionContext.of("value", 1)));
        assertTrue(condition.matches(ConditionContext.of("value", 3)));
        assertFalse(condition.matches(ConditionContext.of("value", 6)));
    }

    @Test
    public void propagatesContextThroughWrappers() {
        Condition condition = Conditions.wrap(
                Condition.of((ctx) -> (double) ctx.mustVar("sqrt") > 2).alias("sqrt > 2"),
                (ctx) -> {
                    // Take value and sqrt it
                    int v = (int) ctx.mustVar("value");
                    return ConditionContext.of("sqrt", Math.sqrt(v));
                }
        );

        assertEquals("sqrt > 2", condition.toString());
        assertTrue(condition.matches(ConditionContext.of("value", 9)));
    }

    @Test
    public void mergesContexts() {
        ConditionContext context = ConditionContext.of("value", 1);
        ConditionContext context1 = ConditionContext.of("random", 2);

        ConditionContext merged = Conditions.mergeContexts(context, context1);

        assertEquals(1, merged.mustVar("value"));
        assertEquals(2, merged.mustVar("random"));
    }

    @Test
    public void mergesContextsOverridesValues() {
        ConditionContext context = ConditionContext.of("value", 1);
        ConditionContext context1 = ConditionContext.of("value", 2);

        ConditionContext merged = Conditions.mergeContexts(context, context1);

        assertEquals(2, merged.mustVar("value"));
    }
}