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

public class DefaultConditions {

    @NotNull
    public static Pair<String, ConditionProvider> tool() {
        return new Pair<>(
                "tool",
                GenericConditionProvider.empty()
                        .addProvider("material", GenericConditionProvider.ProviderEntry.of(
                                (node, key) -> Condition.of((ctx) -> ctx.mustVar("material") == XMaterial.matchXMaterial((String) node)
                                        .orElseThrow(() -> new ParseException("Invalid material " + node))), String.class))
                        .extender((ctx) -> {
                            ItemStack item = (ItemStack) ctx.mustVar("tool");
                            // todo: more - enchants, flags, name, lore
                            return ConditionContext.of("material", XMaterial.matchXMaterial(item));
                        })
        );
    }

    @NotNull
    public static List<Pair<String, ConditionProvider>> all() {
        return Lists.newArrayList(tool());
    }
}
