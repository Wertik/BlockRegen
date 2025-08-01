package nl.aurorion.blockregen.compatibility.impl;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.OraxenMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.Collectors;

public class OraxenProvider extends CompatibilityProvider implements MaterialParser, ItemProvider {

    public OraxenProvider(BlockRegenPlugin plugin) {
        super(plugin, "oraxen");
        setFeatures("materials", "drops", "conditions");
    }

    @Override
    public void onLoad() {
        // Register conditions provider.
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", ((key, node) -> {
            String id = (String) node;

            if (!OraxenItems.exists(id)) {
                throw new ParseException("Invalid Oraxen item '" + id + "'");
            }

            return Condition.of((ctx) -> {
                ItemStack tool = (ItemStack) ctx.mustVar("tool");
                String toolId = OraxenItems.getIdByItem(tool);
                return id.equals(toolId);
            });
        }));
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) {
        if (!OraxenBlocks.isOraxenBlock(input)) {
            throw new ParseException(String.format("'%s' is not an Oraxen block.", input));
        }
        return new OraxenMaterial(this.plugin, input);
    }

    @Override
    public ItemStack createItem(String id, Function<String, String> parser, int amount) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        builder.setDisplayName(parser.apply(builder.getDisplayName()));
        builder.setLore(builder.getLore().stream()
                .map(parser)
                .collect(Collectors.toList()));
        builder.setAmount(amount);
        return builder.build();
    }

    @Override
    public boolean exists(String id) {
        return OraxenItems.exists(id);
    }
}
