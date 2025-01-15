package nl.aurorion.blockregen.preset.condition;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.preset.condition.expression.Constant;
import nl.aurorion.blockregen.preset.condition.expression.Expression;
import nl.aurorion.blockregen.preset.condition.expression.Operand;
import nl.aurorion.blockregen.preset.condition.expression.OperandRelation;
import nl.aurorion.blockregen.util.ParseUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

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
                                        (key, node) -> Condition.of((ctx) -> ctx.mustVar("material") == XMaterial.matchXMaterial((String) node)
                                                .orElseThrow(() -> new ParseException("Invalid material " + node))).alias("material == " + node), String.class))
                                .addProvider("enchants", GenericConditionProvider.ProviderEntry.of(
                                        (key, node) -> {
                                            String v = (String) node;

                                            Matcher matcher = Expression.SYMBOL_PATTERN.matcher(v);

                                            if (!matcher.find()) {
                                                throw new ParseException("Invalid expression " + v);
                                            }

                                            OperandRelation relation = OperandRelation.parse(matcher.group(2));
                                            if (relation == null) {
                                                throw new ParseException("Invalid relation operator.");
                                            }

                                            String left = matcher.group(1);
                                            String right = matcher.group(3);

                                            // One of them has to be a valid enchantment

                                            XEnchantment leftEnchantment = null;
                                            try {
                                                leftEnchantment = ParseUtil.parseEnchantment(left);
                                            } catch (ParseException e) {
                                                //
                                            }
                                            XEnchantment rightEnchantment = null;
                                            try {
                                                rightEnchantment = ParseUtil.parseEnchantment(right);
                                            } catch (ParseException e) {
                                                //
                                            }

                                            if (leftEnchantment == null && rightEnchantment == null) {
                                                throw new ParseException("No enchantment in expression '" + v + "'.");
                                            }

                                            Operand op1;
                                            Operand op2;
                                            if (leftEnchantment == null) {
                                                op1 = new Constant(Operand.Parser.parseObject(left));
                                                op2 = getEnchantmentLevel(rightEnchantment);
                                            } else {
                                                op1 = getEnchantmentLevel(leftEnchantment);
                                                op2 = new Constant(Operand.Parser.parseObject(right));
                                            }

                                            Expression expression = Expression.of(op1, op2, relation);
                                            log.fine(() -> "Loaded enchants expression " + expression);
                                            return Condition.of(expression::evaluate).alias(v);
                                        }, ConditionRelation.AND))
                                .extender((ctx) -> {
                                    ItemStack item = (ItemStack) ctx.mustVar("tool");
                                    return ConditionContext.of(
                                            "material", XMaterial.matchXMaterial(item),
                                            "enchants", item.getEnchantments());
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
    // todo: What if I get two constants? - evaluate at load time
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

    @NotNull
    public static List<Pair<String, GenericConditionProvider.ProviderEntry>> all() {
        return Lists.newArrayList(tool(), placeholder());
    }
}
