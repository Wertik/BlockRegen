package nl.aurorion.blockregen.compatibility.provider;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.items.MythicItem;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.drop.ItemProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MythicMobsProvider extends CompatibilityProvider implements ItemProvider {

    public MythicMobsProvider(BlockRegenPlugin plugin) {
        super(plugin, "mythic");

        setFeatures("drops");
    }

    @Override
    public ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount) {
        Optional<MythicItem> item = MythicBukkit.inst().getItemManager().getItem(id);

        if (!item.isPresent()) {
            return null;
        }

        MythicItem mythicItem = item.get();
        BukkitItemStack itemStack = (BukkitItemStack) mythicItem.generateItemStack(amount);

        // Get the fully rendered Bukkit ItemStack (with CustomModelData and all MythicMobs metadata intact).
        // Modify only name/lore through Bukkit's ItemMeta to avoid wiping CustomModelData,
        // which happened when calling BukkitItemStack.setName/setLore directly.
        ItemStack bukkitStack = itemStack.getItemStack();
        ItemMeta meta = bukkitStack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(parser.apply(meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    meta.setLore(lore.stream().map(parser).collect(Collectors.toList()));
                }
            }
            bukkitStack.setItemMeta(meta);
        }

        return bukkitStack;
    }

    @Override
    public boolean exists(@NotNull String id) {
        return MythicBukkit.inst().getItemManager().getItem(id).isPresent();
    }
}
