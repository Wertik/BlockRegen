package nl.aurorion.blockregen.preset.condition;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.configuration.ParseException;
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

    // A simple comparator expression is all we need. Advanced conditions can be created by composing condition nodes.
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
                            // Parse the expression
                            // todo
                            return Condition.trueCondition();
                        },
                        String.class
                )
        );
    }

    @NotNull
    public static List<Pair<String, GenericConditionProvider.ProviderEntry>> all() {
        return Lists.newArrayList(tool());
    }
}
