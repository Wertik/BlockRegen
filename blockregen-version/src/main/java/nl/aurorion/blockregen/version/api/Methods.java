package nl.aurorion.blockregen.version.api;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Methods {

    default boolean isBarColorValid(@Nullable String string) {
        return false;
    }

    default boolean isBarStyleValid(@Nullable String string) {
        return false;
    }

    @Nullable
    default BossBar createBossBar(@Nullable String text, @Nullable String color, @Nullable String style) {
        return null;
    }

    void setType(@NotNull Block block, @NotNull XMaterial xMaterial);

    /**
     * @throws IllegalArgumentException If the block is invalid.
     */
    @NotNull
    XMaterial getType(@NotNull Block block);

    default boolean compareType(@NotNull Block block, @NotNull XMaterial xMaterial) {
        return getType(block) == xMaterial;
    }

    @NotNull
    ItemStack getItemInMainHand(@NotNull Player player);

    void handleDropItemEvent(Player player, BlockState blockState, List<Item> items);

    int applyMending(Player player, int experience);

    @NotNull
    Item createDroppedItem(@NotNull Location location, @NotNull ItemStack item);
}
