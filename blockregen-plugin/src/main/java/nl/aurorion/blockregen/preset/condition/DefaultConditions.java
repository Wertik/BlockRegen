package nl.aurorion.blockregen.preset.condition;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.configuration.ParseException;
import nl.aurorion.blockregen.preset.condition.expression.Expression;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Default conditions provided by this plugin.
 */
public class DefaultConditions {

    @NotNull
    public static Pair<String, GenericConditionProvider.ProviderEntry> tool() {
        return new Pair<>(
                "tool",
                GenericConditionProvider.ProviderEntry.provider(
                        GenericConditionProvider.empty()
                                .addProvider("material", GenericConditionProvider.ProviderEntry.of(
                                        (key, node) -> Condition.of((ctx) -> ctx.mustVar("material") == XMaterial.matchXMaterial((String) node)
                                                .orElseThrow(() -> new ParseException("Invalid material " + node))), String.class))
                                .extender((ctx) -> {
                                    ItemStack item = (ItemStack) ctx.mustVar("tool");
                                    // todo: more - enchants, flags, name, lore
                                    return ConditionContext.of("material", XMaterial.matchXMaterial(item));
                                }))
        );
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
                            return Condition.of(expression::evaluate);
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
