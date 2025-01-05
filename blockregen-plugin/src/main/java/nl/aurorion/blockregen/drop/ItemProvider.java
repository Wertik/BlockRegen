package nl.aurorion.blockregen.drop;

import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

// Providers items to drop.
public interface ItemProvider {

    // Provide an ItemStack. Run all strings through the parser.
    ItemStack createItem(String id, Function<String, String> parser, int amount);

    // Verify that this item exists.
    boolean exists(String id);
}
