package nl.aurorion.blockregen.compatibility.provider;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.material.NexoMaterial;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public class NexoProvider extends CompatibilityProvider implements MaterialProvider {

    public NexoProvider(BlockRegenPlugin plugin) {
        super(plugin, "nexo");
        setFeatures("materials", "conditions");
    }

    @Override
    public void onLoad() {
        // Register conditions provider.
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", ((key, node) -> {
            String id = (String) node;

            if (!NexoItems.exists(id)) {
                throw new ParseException("Invalid Nexo item '" + id + "'");
            }

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
        return new NexoMaterial(input);
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NonNull Block block) {
        if (!NexoBlocks.isCustomBlock(block)) {
            return null;
        }

        CustomBlockMechanic customBlockMechanic = NexoBlocks.customBlockMechanic(block.getBlockData());
        if (customBlockMechanic == null) {
            return null;
        }
        return new NexoMaterial(customBlockMechanic.getItemID());
    }

    @Override
    public @NonNull Class<?> getClazz() {
        return NexoMaterial.class;
    }

    @Override
    public BlockRegenMaterial createInstance(Type type) {
        return new NexoMaterial(null);
    }
}
