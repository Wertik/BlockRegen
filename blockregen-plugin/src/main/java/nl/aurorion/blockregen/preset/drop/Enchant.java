package nl.aurorion.blockregen.preset.drop;

import com.cryptomorin.xseries.XEnchantment;
import lombok.Getter;
import nl.aurorion.blockregen.util.ParseUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Enchant {

    @Getter
    private final XEnchantment enchantment;
    @Getter
    private final int level;

    public Enchant(XEnchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    @Nullable
    public static Enchant from(String str) {
        String enchantString = str;
        int level = 1;

        if (str.contains(":")) {
            String[] arr = str.split(":");
            if (arr.length == 2) {
                enchantString = arr[0];
                level = ParseUtil.parseInt(arr[1], 1);
            }
        }

        XEnchantment xEnchantment = ParseUtil.parseEnchantment(enchantString);

        if (xEnchantment == null)
            return null;

        return new Enchant(xEnchantment, level);
    }

    public static Set<Enchant> load(List<String> input) {
        if (input.isEmpty())
            return new HashSet<>();

        Set<Enchant> out = new HashSet<>();
        for (String str : input) {
            Enchant enchant = from(str);
            if (enchant != null)
                out.add(enchant);
        }
        return out;
    }

    public void apply(ItemMeta meta) {
        if (meta == null)
            return;

        Enchantment enchantment = this.enchantment.getEnchant();

        if (enchantment == null)
            return;

        meta.addEnchant(enchantment, level, true);
    }
}
