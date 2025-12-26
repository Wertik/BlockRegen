package nl.aurorion.blockregen.compatibility.provider;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MMOItemsProvider extends CompatibilityProvider implements MaterialProvider {
    public MMOItemsProvider(BlockRegenPlugin plugin) {
        super(plugin, "mmoitems");
        setFeatures("materials", "conditions");
    }

    private static final Pattern ITEM_PATTERN = Pattern.compile("(\\S+):(\\S+)");

    @Override
    public void onLoad() {
        // Register conditions provider.
        // https://gitlab.com/phoenix-dvpmt/mmoitems/-/wikis/Main%20API%20Features#checking-if-an-itemstack-is-from-mi
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", ((key, node) -> {

            Matcher matcher = ITEM_PATTERN.matcher((String) node);

            if (!matcher.matches()) {
                throw new ParseException("Invalid input for MMOItems tool. Has to have the format of <type>:<id>.");
            }

            String typeName = matcher.group(1);

            Type type = MMOItems.plugin.getTypes().get(typeName);

            if (type == null) {
                throw new ParseException("Invalid MMOItems item type " + typeName + ".");
            }

            String id = matcher.group(2);

            MMOItem item = MMOItems.plugin.getMMOItem(type, id);

            if (item == null) {
                throw new ParseException("Invalid MMOItems item '" + type + ":" + id + "'.");
            }

            return Condition.of((ctx) -> {
                ItemStack tool = (ItemStack) ctx.mustVar("tool");
                NBTItem nbtItem = NBTItem.get(tool);

                if (nbtItem == null) {
                    return false;
                }

                if (!nbtItem.hasType() || !nbtItem.hasTag("MMOITEMS_ITEM_ID")) {
                    return false;
                }

                String toolType = nbtItem.getType();

                if (!toolType.equalsIgnoreCase(typeName)) {
                    return false;
                }

                String toolId = nbtItem.getString("MMOITEMS_ITEM_ID");

                if (!toolId.equalsIgnoreCase(id)) {
                    return false;
                }

                return true;
            });
        }));
    }

    /**
     * @throws ParseException If parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) {
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new ParseException(String.format("Invalid MMOItem block id: '%s'.", input));
        }

        CustomBlock customBlock = MMOItems.plugin.getCustomBlocks().getBlock(id);

        if (customBlock == null) {
            throw new ParseException("Invalid MMOItems block '" + input + "'");
        }

        return new MMOIItemsMaterial(plugin, id);
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NonNull Block block) {
        Optional<CustomBlock> fromBlock = MMOItems.plugin.getCustomBlocks().getFromBlock(block.getBlockData());
        return fromBlock.map(customBlock -> new MMOIItemsMaterial(plugin, customBlock.getId())).orElse(null);

    }

    @Override
    public @NonNull Class<?> getClazz() {
        return MMOIItemsMaterial.class;
    }

    @Override
    public BlockRegenMaterial createInstance(java.lang.reflect.Type type) {
        return new MMOIItemsMaterial(plugin, -1);
    }
}
