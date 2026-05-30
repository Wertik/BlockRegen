package nl.aurorion.blockregen.compatibility.provider;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.items.MythicItem;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.drop.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

        // MythicMobs item generation is not thread-safe and must run on the main thread.
        // If called from an async context (e.g. BlockRegen's reward processing), dispatch
        // to the main thread and block until the result is ready.
        if (!Bukkit.isPrimaryThread()) {
            CompletableFuture<ItemStack> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> future.complete(buildItemStack(mythicItem, parser, amount)));
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException e) {
                return null;
            }
        }

        return buildItemStack(mythicItem, parser, amount);
    }

    private ItemStack buildItemStack(MythicItem mythicItem, Function<String, String> parser, int amount) {
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
