package nl.aurorion.blockregen.compatibility.provider;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.RegenerationContextKey;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.ProviderFeatureFlag;
import nl.aurorion.blockregen.drop.ItemProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log
public class EcoItemsProvider extends CompatibilityProvider implements ItemProvider {

    public EcoItemsProvider(BlockRegenPlugin plugin) {
        super(plugin, "eco", "ecoitems");
        setFeatures(ProviderFeatureFlag.DROPS);
    }

    @Override
    public ItemStack createItem(@NotNull String id, int amount, @NotNull Context ctx) {
        EcoItem ecoItem = EcoItems.INSTANCE.getByID(id);
        if (ecoItem == null) {
            return null;
        }
        return buildItemStack(ecoItem, ctx, amount);
    }

    private ItemStack buildItemStack(EcoItem ecoItem, Context ctx, int amount) {
        @SuppressWarnings("unchecked")
        Function<String, String> parser = (Function<String, String>) ctx.mustVar(RegenerationContextKey.PARSER, Function.class);

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
        log.fine(() -> "get by id " + id);
        return EcoItems.INSTANCE.getByID(id) != null;
    }
}
