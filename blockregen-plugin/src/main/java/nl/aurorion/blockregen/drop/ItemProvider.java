package nl.aurorion.blockregen.drop;

import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.RegenerationContextKey;
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
    default ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount) {
        Context ctx = Context.of(RegenerationContextKey.PARSER, parser);
        return createItem(id, amount, ctx);
    }

    @Nullable
    ItemStack createItem(@NotNull String id, int amount, @NotNull Context context);

    // Verify that this item exists.
    boolean exists(@NotNull String id);
}
