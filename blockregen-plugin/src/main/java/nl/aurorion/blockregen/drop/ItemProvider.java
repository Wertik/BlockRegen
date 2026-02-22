package nl.aurorion.blockregen.drop;

import nl.aurorion.blockregen.Context;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

// Providers items to drop.
public interface ItemProvider {

    // Provide an ItemStack. Run all strings through the parser (lore, name).
    @Nullable
    // Deprecated: use #createItem(Context)
    @Deprecated
    ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount);

    @SuppressWarnings("unchecked")
    @Nullable
    default ItemStack createItem(@NotNull String id, int amount, @NotNull Context context) {
        return createItem(id, (Function<String, String>) context.mustVar("parser"), amount);
    }

    // Verify that this item exists.
    boolean exists(@NotNull String id);
}
