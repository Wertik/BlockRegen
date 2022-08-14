package nl.aurorion.blockregen.system.preset.struct.drop;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.util.ParseUtil;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log
public class ItemDrop {

    @Getter
    private final XMaterial material;

    @Getter
    @Setter
    private Amount amount = new Amount(1);

    @Getter
    @Setter
    private String displayName;

    @Getter
    @Setter
    private List<String> lore = new ArrayList<>();

    @Getter
    @Setter
    private Set<Enchant> enchants = new HashSet<>();

    @Getter
    @Setter
    private Set<ItemFlag> itemFlags = new HashSet<>();

    @Getter
    @Setter
    private boolean dropNaturally = true;

    @Getter
    @Setter
    private ExperienceDrop experienceDrop;

    @Getter
    @Setter
    private Amount chance;

    public ItemDrop(XMaterial material) {
        this.material = material;
    }

    @Nullable
    public ItemStack toItemStack(@Nullable Player player) {

        // x/100% chance to drop
        if (chance != null) {
            int threshold = chance.getInt();
            int roll = BlockRegen.getInstance().getRandom().nextInt(101);

            if (roll > threshold) {
                log.fine(String.format("Drop %s failed chance roll, %d > %d", this, roll, threshold));
                return null;
            }
        }

        int amount = this.amount.getInt();

        if (amount <= 0) return null;

        ItemStack itemStack = material.parseItem();

        if (itemStack == null)
            return null;

        itemStack.setAmount(amount);

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) return null;

        if (displayName != null) {
            itemMeta.setDisplayName(StringUtil.color(TextUtil.parse(displayName, player)));
        }

        if (lore != null) {
            List<String> lore = new ArrayList<>(this.lore);

            lore.replaceAll(o -> StringUtil.color(TextUtil.parse(o, player)));

            itemMeta.setLore(lore);
        }

        enchants.forEach(enchant -> enchant.apply(itemMeta));
        itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    @Nullable
    public static ItemDrop load(ConfigurationSection section, BlockPreset preset) {

        if (section == null)
            return null;

        XMaterial material = ParseUtil.parseMaterial(section.getString("material"));

        if (material == null) {
            log.warning("Could not load item drop at " + section.getCurrentPath() + ", material is invalid.");
            return null;
        }

        ItemDrop drop = new ItemDrop(material);

        drop.setAmount(Amount.load(section, "amount", 1));
        drop.setDisplayName(section.getString("name"));
        drop.setLore(section.getStringList("lores"));

        drop.setEnchants(Enchant.load(section.getStringList("enchants")));
        drop.setItemFlags(section.getStringList("flags").stream()
                .map(str -> ParseUtil.parseEnum(str, ItemFlag.class,
                        e -> log.warning("Could not parse ItemFlag from " + str)))
                .collect(Collectors.toSet()));

        drop.setDropNaturally(section.getBoolean("drop-naturally", preset.isDropNaturally()));

        drop.setExperienceDrop(ExperienceDrop.load(section.getConfigurationSection("exp"), drop));
        drop.setChance(Amount.load(section, "chance", 100));

        return drop;
    }
}