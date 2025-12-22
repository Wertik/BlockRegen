package nl.aurorion.blockregen.util;

import com.cryptomorin.xseries.XEnchantment;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Log
@UtilityClass
public class Items {

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    private int quantityDropped(Material mat) {
        return mat == Material.LAPIS_ORE ? 4 + BlockRegenPluginImpl.getInstance().getRandom().nextInt(5) : 1;
    }

    /**
     * Get the quantity dropped based on the given fortune level
     */
    public int applyFortune(Material mat, ItemStack tool) {
        Enchantment fortune = Objects.requireNonNull(XEnchantment.FORTUNE.get(), "Could not parse fortune enchantment into this version.");

        if (tool.getItemMeta() == null || !tool.getItemMeta().hasEnchants() ||
                !tool.getItemMeta().hasEnchant(fortune))
            return 0;

        int fortuneLevel = tool.getItemMeta().getEnchantLevel(fortune);

        if (fortuneLevel > 0) {
            int i = BlockRegenPluginImpl.getInstance().getRandom().nextInt(fortuneLevel + 2) - 1;

            if (i < 0) i = 0;

            return quantityDropped(mat) * i;
        } else return quantityDropped(mat);
    }
}