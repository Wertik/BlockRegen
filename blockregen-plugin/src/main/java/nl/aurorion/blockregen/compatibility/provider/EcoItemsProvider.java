package nl.aurorion.blockregen.compatibility.provider;

import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.drop.ItemProvider;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;

public class EcoItemsProvider extends CompatibilityProvider implements ItemProvider {

    public EcoItemsProvider(BlockRegenPlugin plugin) {
        super(plugin, "ecoitems");

        setFeatures("drops");
    }

    @Override
    public ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount) {
        EcoItem ecoItem = EcoItems.INSTANCE.getByID(id);
        if(ecoItem == null) return null;
        return buildItemStack(ecoItem, parser, amount);
    }

    private ItemStack buildItemStack(EcoItem ecoItem, Function<String, String> parser, int amount) {
        ItemStack bukkitStack = ecoItem.getItemStack();
        bukkitStack.setAmount(amount);

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
        return EcoItems.INSTANCE.getByID(id) != null;
    }
}
