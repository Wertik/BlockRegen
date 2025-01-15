package nl.aurorion.blockregen.version.current;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.Colors;
import nl.aurorion.blockregen.version.api.Methods;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Log
public class LatestMethods implements Methods {

    @Override
    public boolean isBarColorValid(@Nullable String string) {
        return parseColor(string) != null;
    }

    @Override
    @Nullable
    public BossBar createBossBar(@Nullable String text, @Nullable String color, @Nullable String style) {
        BarColor barColor = parseColor(color);
        BarStyle barStyle = parseStyle(style);
        if (barColor == null || barStyle == null)
            return null;
        return Bukkit.createBossBar(Colors.color(text), barColor, barStyle);
    }

    @Override
    public boolean isBarStyleValid(@Nullable String string) {
        return parseStyle(string) != null;
    }

    @Nullable
    private BarStyle parseStyle(@Nullable String str) {
        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        try {
            return BarStyle.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private BarColor parseColor(@Nullable String str) {
        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        try {
            return BarColor.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void handleDropItemEvent(Player player, BlockState blockState, List<Item> items) {
        BlockDropItemEvent event = new BlockDropItemEvent(blockState.getBlock(), blockState, player, new ArrayList<>(items));
        Bukkit.getPluginManager().callEvent(event);

        // Delete the entities if any other plugins cancel the event or clear the drops.
        // Otherwise, we get duplicated drops from enchantment plugins.
        // Note: This means that any changes a plugin applies to the items is not going to be reflected on the drops.
        // Note: I am not sure how to make that work, nor whether it should be a thing.

        if (event.isCancelled() || event.getItems().isEmpty()) {
            log.fine(() -> "Drops got cancelled.");
            items.forEach(Entity::remove);
        }
    }

    @Override
    public void setType(@NotNull Block block, @NotNull XMaterial xMaterial) {
        XBlock.setType(block, xMaterial);
    }

    @Override
    public XMaterial getType(@NotNull Block block) {
        return XMaterial.matchXMaterial(block.getType());
    }

    @Override
    public ItemStack getItemInMainHand(@NotNull Player player) {
        return player.getInventory().getItemInMainHand();
    }
}
