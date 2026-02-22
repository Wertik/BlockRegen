package nl.aurorion.blockregen.compatibility.provider;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.material.MMOItemsMaterial;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import nl.aurorion.blockregen.util.Text;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MMOItemsProvider extends CompatibilityProvider implements ItemProvider, MaterialProvider {

    private static final Pattern ITEM_PATTERN = Pattern.compile("(\\S+):(\\S+)");

    public MMOItemsProvider(BlockRegenPlugin plugin) {
        super(plugin, "mmoitems");
        setFeatures("materials", "conditions", "drops");
    }

    @Override
    public void onLoad() {
        // Register conditions provider.
        // https://docs.phoenixdevt.fr/mmoitems/api/main.html#checking-if-an-itemstack-is-from-mi
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", ((key, node) -> {
            final MMOItem item = getMMOItem((String) node);

            if (item == null) {
                throw new ParseException("Invalid MMOItems item '" + node + "'.", true);
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

                if (!toolType.equalsIgnoreCase(item.getType().getName())) {
                    return false;
                }

                String toolId = nbtItem.getString("MMOITEMS_ITEM_ID");

                if (!toolId.equalsIgnoreCase(item.getId())) {
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
    public @NotNull BlockRegenMaterial parseMaterial(@NotNull String input) {
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new ParseException(String.format("Invalid MMOItem block id: '%s'.", input));
        }

        CustomBlock customBlock = MMOItems.plugin.getCustomBlocks().getBlock(id);

        if (customBlock == null) {
            throw new ParseException("Invalid MMOItems block '" + input + "'", true);
        }

        return new MMOItemsMaterial(id);
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NotNull Block block) {
        Optional<CustomBlock> fromBlock = MMOItems.plugin.getCustomBlocks().getFromBlock(block.getBlockData());
        return fromBlock.map(customBlock -> new MMOItemsMaterial(customBlock.getId())).orElse(null);

    }

    @Override
    public @NotNull Class<?> getClazz() {
        return MMOItemsMaterial.class;
    }

    @Override
    public BlockRegenMaterial createInstance(java.lang.reflect.Type type) {
        return new MMOItemsMaterial(-1);
    }

    @Override
    public @Nullable ItemStack createItem(@NonNull String id, int amount, @NonNull Context context) {
        final Player player = context.get("player", Player.class);
        final Block block = context.get("block", Block.class);

        final MMOItem mmoItem = getMMOItem(id, player);

        if (mmoItem == null) {
            return null;
        }

        ItemStackBuilder itemBuilder = mmoItem.newBuilder();
        itemBuilder.getLore().setLore(itemBuilder.getLore().getLore().stream()
                .map(s -> Text.parse(s, player, block))
                .collect(Collectors.toList()));

        if (itemBuilder.getMeta().hasDisplayName()) {
            itemBuilder.getMeta().setDisplayName(Text.parse(itemBuilder.getMeta().getDisplayName(), player, block));
        }

        ItemStack itemStack = itemBuilder.build();
        if (itemStack != null) {
            itemStack.setAmount(amount);
        }

        return itemStack;
    }

    @Override
    public @Nullable ItemStack createItem(@NonNull String id, @NonNull Function<String, String> parser, int amount) {
        // not called unless the other one is unimplemented
        return null;
    }

    @Override
    public boolean exists(@NotNull String id) {
        return getMMOItem(id) != null;
    }

    private MMOItem getMMOItem(@NotNull String id) {
        return getMMOItem(id, null);
    }

    @Nullable
    private MMOItem getMMOItem(@NotNull String id, @Nullable Player player) {
        Matcher matcher = ITEM_PATTERN.matcher(id);

        if (!matcher.matches()) {
            throw new ParseException("Invalid input for MMOItem. Has to have the format of '<type>:<id>'.");
        }

        String typeName = matcher.group(1).toUpperCase();
        Type type = MMOItems.plugin.getTypes().get(typeName);

        if (type == null) {
            throw new ParseException("Invalid MMOItems item type '" + typeName + "'.");
        }

        String itemId = matcher.group(2).toUpperCase();

        if (player == null) {
            return MMOItems.plugin.getMMOItem(type, itemId);
        }

        PlayerData playerData = MMOItems.plugin.getPlayerDataManager().get(player);

        return MMOItems.plugin.getMMOItem(type, itemId, playerData);
    }
}
