package nl.aurorion.blockregen.drop;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

// Providers items to drop.
public interface ItemProvider {

    // Provide an ItemStack. Run all strings through the parser (lore, name).
    @Nullable
    ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount);

    // Verify that this item exists.
    boolean exists(@NotNull String id);
}
