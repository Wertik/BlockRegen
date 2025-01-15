package nl.aurorion.blockregen.compatibility.impl;

import com.linecorp.conditional.Condition;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.NexoMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NexoProvider extends CompatibilityProvider implements MaterialParser {
    public NexoProvider(BlockRegenPlugin plugin) {
        super(plugin, "nexo");
        setFeatures("materials", "conditions");
    }

    @Override
    public void onLoad() {
        // Register conditions provider.
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", ((key, node) -> {
            String id = (String) node;
            return Condition.of((ctx) -> {
                ItemStack tool = (ItemStack) ctx.mustVar("tool");
                String toolId = NexoItems.idFromItem(tool);
                return id.equals(toolId);
            });
        }));
    }

    /**
     * @throws ParseException If parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) {
        if (!NexoBlocks.isCustomBlock(input)) {
            throw new ParseException(String.format("'%s' is not a Nexo block.", input));
        }
        return new NexoMaterial(this.plugin, input);
    }
}
