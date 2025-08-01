package nl.aurorion.blockregen.preset.condition;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.conditional.ConditionContext;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.preset.FixedNumberValue;
import nl.aurorion.blockregen.preset.NumberValue;
import nl.aurorion.blockregen.preset.condition.expression.Expression;
import nl.aurorion.blockregen.preset.condition.expression.Operand;
import nl.aurorion.blockregen.util.Parsing;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Default conditions provided by this plugin.
 */
@Log
public class DefaultConditions {

    @NotNull
    public static Pair<String, GenericConditionProvider.ProviderEntry> tool() {
        return new Pair<>(
                "tool",
                GenericConditionProvider.ProviderEntry.of(
                        GenericConditionProvider.empty()
                                .addProvider("material", GenericConditionProvider.ProviderEntry.of(
                                        (key, node) -> {
                                            XMaterial xMaterial = Parsing.parseMaterial((String) node);

                                            return Condition.of((ctx) -> ctx.mustVar("material") == xMaterial)
                                                    .alias("material == " + xMaterial);
                                        }, String.class))
                                .addProvider("enchants", GenericConditionProvider.ProviderEntry.of(
                                        (key, node) -> {
                                            String v = (String) node;

                                            Expression expression = Expression.withCustomOperands((str) -> getEnchantmentLevel(Parsing.parseEnchantment(str)), v);

                                            log.fine(() -> "Loaded enchants expression " + expression);
                                            return Condition.of(expression::evaluate).alias(v);
                                        }, ConditionRelation.AND))
                                .extender((ctx) -> {
                                    ItemStack item = (ItemStack) ctx.mustVar("tool");
                                    return ConditionContext.empty()
                                            .with("material", XMaterial.matchXMaterial(item))
                                            .with("enchants", item.getEnchantments());
                                }), ConditionRelation.AND)
        );
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Operand getEnchantmentLevel(XEnchantment xEnchantment) {
        Operand op1;
        op1 = ctx -> {
            Map<Enchantment, Integer> enchants = (Map<Enchantment, Integer>) ctx.mustVar("enchants");
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                if (entry.getKey() == xEnchantment.get()) {
                    return entry.getValue();
                }
            }
            return 0;
        };
        return op1;
    }

    // Expressions have two sides, either of them can be constant.
    // The types have to be figured out at execution time (when to expression is evaluated)
    // examples:
    // "%player_y% > 20"
    // "30 > %player_y%"
    // "%player_y%" - assume != 0
    // operators: <, <=, >=, >, ==, !=
    @NotNull
    public static Pair<String, GenericConditionProvider.ProviderEntry> placeholder() {
        return new Pair<>(
                "placeholder",
                GenericConditionProvider.ProviderEntry.of(
                        (key, node) -> {
                            String input = (String) node;

                            Expression expression = Expression.from(input);
                            return Condition.of(expression::evaluate).alias(expression.pretty());
                        },
                        String.class
                )
        );
    }

    // A uniform chance.
    @NotNull
    public static Pair<String, GenericConditionProvider.ProviderEntry> chance() {
        return new Pair<>(
                "chance",
                GenericConditionProvider.ProviderEntry.of(
                        (key, node) -> {
                            final NumberValue numberValue;
                            try {
                                numberValue = FixedNumberValue.from(node);
                            } catch (IllegalArgumentException e) {
                                throw new ParseException(e.getMessage());
                            }
                            final Random random = new Random();
                            return Condition.of((ctx) -> random.nextDouble() < numberValue.getDouble() / 100).alias("chance (" + numberValue + "%)");
                        },
                        Double.class, Integer.class, String.class
                )
        );
    }

    @NotNull
    public static List<Pair<String, GenericConditionProvider.ProviderEntry>> all() {
        return Lists.newArrayList(tool(), placeholder(), chance());
    }
}
