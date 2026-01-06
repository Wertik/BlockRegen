package nl.aurorion.blockregen.compatibility.provider;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.items.MythicItem;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.drop.ItemProvider;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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

        itemStack.setLore(mythicItem.getLore().stream().map(parser).collect(Collectors.toList()));
        itemStack.setName(parser.apply(mythicItem.getDisplayName()));

        return itemStack.getItemStack();
    }

    @Override
    public boolean exists(@NotNull String id) {
        return MythicBukkit.inst().getItemManager().getItem(id).isPresent();
    }
}
