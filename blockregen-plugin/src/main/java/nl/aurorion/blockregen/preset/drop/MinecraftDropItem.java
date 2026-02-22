package nl.aurorion.blockregen.preset.drop;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.util.Colors;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Getter
@Log
public class MinecraftDropItem extends DropItem {

    private final XMaterial material;

    @Setter
    private String displayName;

    @Setter
    private List<String> lore = new ArrayList<>();

    @Setter
    private Set<Enchant> enchants = new HashSet<>();

    @Setter
    private Set<ItemFlag> itemFlags = new HashSet<>();

    @Setter
    private Integer customModelData;
    @Setter
    private NamespacedKey itemModel;

    public MinecraftDropItem(XMaterial material) {
        this.material = material;
    }

    /**
     * Compose this Drop into an item stack.
     *
     * @return Created item stack.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public ItemStack toItemStack(Context context) {
        int amount = this.amount.getInt();
        if (amount <= 0) {
            return null;
        }

        ItemStack itemStack = material.parseItem();

        if (itemStack == null) {
            return null;
        }

        itemStack.setAmount(amount);

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) {
            return null;
        }

        final Function<String, String> parser = (Function<String, String>) context.mustVar("parser");

        if (displayName != null) {
            itemMeta.setDisplayName(Colors.color(parser.apply(displayName)));
        }

        if (lore != null) {
            List<String> lore = new ArrayList<>(this.lore);

            lore.replaceAll(o -> Colors.color(parser.apply(o)));

            itemMeta.setLore(lore);
        }

        enchants.forEach(enchant -> enchant.apply(itemMeta));
        itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        // On 1.14+, apply custom model data
        if (BlockRegenPluginImpl.getInstance().getVersionManager().useCustomModelData()) {
            // Add PDC with custom model data
            if (customModelData != null) {
                itemMeta.setCustomModelData(customModelData);
                log.fine(() -> String.format("Setting custom model data of %d", customModelData));
            }
        }

        if (itemModel != null) {
            itemMeta.setItemModel(itemModel);
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    @Override
    public String toString() {
        return "ItemDrop{" +
                "material=" + material +
                ", amount=" + amount +
                ", displayName='" + displayName + '\'' +
                ", lore=" + lore +
                ", enchants=" + enchants +
                ", itemFlags=" + itemFlags +
                ", dropNaturally=" + dropNaturally +
                ", experienceDrop=" + experienceDrop +
                ", chance=" + chance +
                ", customModelData=" + customModelData +
                ", itemModel=" + itemModel +
                ", condition=" + condition +
                '}';
    }
}