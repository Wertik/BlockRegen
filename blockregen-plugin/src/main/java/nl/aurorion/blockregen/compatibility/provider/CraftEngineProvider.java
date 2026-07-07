package nl.aurorion.blockregen.compatibility.provider;

import io.th0rgal.oraxen.api.OraxenItems;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.RegenerationContextKey;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.ProviderFeatureFlag;
import nl.aurorion.blockregen.compatibility.material.CraftEngineMaterial;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public class CraftEngineProvider extends CompatibilityProvider implements ItemProvider, MaterialProvider {

    public CraftEngineProvider(BlockRegenPlugin plugin) {
        super(plugin, "ce", "craftengine");
        setFeatures(ProviderFeatureFlag.MATERIALS, ProviderFeatureFlag.DROPS);
    }

    @Override
    public void onLoad() {
        // Register conditions provider.
        for (String prefix : getPrefixes()) {
            plugin.getPresetManager().getConditions().addProvider(prefix + "/tool", ((key, node) -> {
                String id = (String) node;

                if (!OraxenItems.exists(id)) {
                    throw new ParseException("Invalid CraftEngine item '" + id + "'", true);
                }

                return Condition.of((ctx) -> {
                    ItemStack tool = (ItemStack) ctx.mustVar(RegenerationContextKey.TOOL);

                    BukkitItemDefinition craftEngineItem = CraftEngineItems.byItemStack(tool);
                    if (craftEngineItem == null) {
                        return false;
                    }
                    Key toolId = craftEngineItem.id();
                    return id.equals(toolId.asString());
                });
            }));
        }
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(@NotNull String input) {
        Key id = Key.ce(input);
        BlockDefinition blockDefinition = CraftEngineBlocks.byId(id);
        if (blockDefinition == null) {
            throw new ParseException(String.format("'%s' ('%s') is not a CraftEngine block.", input, id), true);
        }
        return new CraftEngineMaterial(id);
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NotNull Block block) {
        BukkitExistingBlock existingBlock = BukkitAdaptor.adapt(block);
        if (!existingBlock.isCustom()) {
            return null;
        }
        return new CraftEngineMaterial(existingBlock.id());
    }

    @Override
    public @NotNull Class<?> getClazz() {
        return CraftEngineMaterial.class;
    }

    @Override
    public ItemStack createItem(@NotNull String id, int amount, @NotNull Context ctx) {
        BukkitItemDefinition bukkitItemDefinition = CraftEngineItems.byId(id);
        if (bukkitItemDefinition == null) {
            return null;
        }

        Player player = (Player) ctx.mustVar(RegenerationContextKey.PLAYER);

        ItemBuildContext buildCtx = ItemBuildContext.of(BukkitAdaptor.adapt(player), ContextHolder.builder()
                .withParameter(DirectContextParameters.BLOCK, BukkitAdaptor.adapt(ctx.mustVar(RegenerationContextKey.BLOCK, Block.class)))
                .build());
        BukkitItem item = bukkitItemDefinition.buildItem(buildCtx, amount);

        // todo: use an ItemProcessor? to parse in what we need?
        // rn too much effort for little reward, we already pass in the context

        return item.getBukkitItem();
    }

    @Override
    public boolean exists(@NotNull String id) {
        return CraftEngineItems.byId(id) != null;
    }

    @Override
    public BlockRegenMaterial createInstance(Type type) {
        return new CraftEngineMaterial(null);
    }

    @Override
    public boolean containsColon() {
        return true;
    }
}
