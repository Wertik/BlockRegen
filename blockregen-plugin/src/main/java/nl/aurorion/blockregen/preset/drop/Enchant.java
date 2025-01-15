package nl.aurorion.blockregen.preset.drop;

import com.cryptomorin.xseries.XEnchantment;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.util.Parsing;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log
public class Enchant {

    @Getter
    private final XEnchantment enchantment;
    @Getter
    private final int level;

    public Enchant(XEnchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public static Enchant from(String str) {
        String enchantString = str;
        int level = 1;

        if (str.contains(":")) {
            String[] arr = str.split(":");
            if (arr.length == 2) {
                enchantString = arr[0];
                level = Parsing.parseInt(arr[1], 1);
            }
        }

        XEnchantment xEnchantment = Parsing.parseEnchantment(enchantString);

        return new Enchant(xEnchantment, level);
    }

    /**
     * @throws ParseException If parsing fails.
     */
    @NotNull
    public static Set<Enchant> loadSet(@NotNull List<String> input) {
        Set<Enchant> out = new HashSet<>();
        for (String str : input) {
            Enchant enchant = Enchant.from(str);
            out.add(enchant);
        }
        return out;
    }

    /**
     * Add enchant to ItemMeta.
     */
    public void apply(@Nullable ItemMeta meta) {
        if (meta == null) {
            return;
        }

        Enchantment enchantment = this.enchantment.get();
        if (enchantment == null) {
            log.warning("Unable to parse XEnchantment '" + this.enchantment + "' to this version.");
            return;
        }
        meta.addEnchant(enchantment, level, true);
    }
}
