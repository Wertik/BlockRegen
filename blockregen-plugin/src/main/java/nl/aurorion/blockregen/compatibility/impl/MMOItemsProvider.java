package nl.aurorion.blockregen.compatibility.impl;

import com.linecorp.conditional.Condition;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MMOItemsProvider extends CompatibilityProvider implements MaterialParser {
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
            String id;
            boolean powerPickaxe;
            if (typeName.equalsIgnoreCase("powerpickaxe")) {
                id = matcher.group(2);
                powerPickaxe = true;
            } else {
                powerPickaxe = false;
                Type type = MMOItems.plugin.getTypes().get(typeName);
                if (type == null) {
                    throw new ParseException("Invalid MMOItems item type " + typeName + ".");
                }

                id = matcher.group(2);

                MMOItem item = MMOItems.plugin.getMMOItem(type, id);

                if (item == null) {
                    throw new ParseException("Invalid MMOItems item '" + type + ":" + id + "'.");
                }
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
                if (powerPickaxe) {
                    if (nbtItem.hasTag("MMOITEMS_PICKAXE_POWER")) {
                        return Integer.parseInt(id) >= nbtItem.getInteger("MMOITEMS_PICKAXE_POWER");
                    }
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
}
