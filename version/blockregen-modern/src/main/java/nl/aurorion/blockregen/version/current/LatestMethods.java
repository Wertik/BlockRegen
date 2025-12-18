package nl.aurorion.blockregen.version.current;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.Colors;
import nl.aurorion.blockregen.version.api.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Log
public class LatestMethods implements Methods {

    private final EquipmentSlot[] PLAYER_EQUIPMENT_SLOTS = {
            EquipmentSlot.CHEST,
            EquipmentSlot.FEET,
            EquipmentSlot.HAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.LEGS,
            EquipmentSlot.OFF_HAND
    };

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
        BlockDropItemEvent event = new BlockDropItemEvent(blockState.getBlock(), blockState, player, items);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            items.stream().filter(Entity::isValid).forEach(Entity::remove);
            return;
        }

        items.stream().filter(item -> !item.isValid()).forEach((item) -> item.getWorld().addEntity(item));
    }

    @Override
    public void setType(@NotNull Block block, @NotNull XMaterial xMaterial) {
        XBlock.setType(block, xMaterial, false);
    }

    @Override
    public @NotNull XMaterial getType(@NotNull Block block) {
        return XMaterial.matchXMaterial(block.getType());
    }

    @Override
    public @NotNull ItemStack getItemInMainHand(@NotNull Player player) {
        return player.getInventory().getItemInMainHand();
    }

    @Nullable
    private ItemStack getRepairableItem(@NotNull Player player) {
        Enchantment mending = XEnchantment.MENDING.get();

        if (mending == null) {
            log.fine("Mending not supported");
            return null;
        }

        for (EquipmentSlot slot : PLAYER_EQUIPMENT_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);

            if (item == null) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            // Damageable added in 1.13
            if (!(meta instanceof Damageable)) {
                continue;
            }

            Damageable damageable = (Damageable) meta;

            if (!damageable.hasDamage()) {
                continue;
            }

            if (meta.getEnchantLevel(mending) > 0) {
                return item;
            }
        }
        return null;
    }

    /**
     * Apply the mending enchantment to the players items.
     *
     * @return experience How much experience wasn't used up by mending and should be added to the players exp.
     */
    @Override
    public int applyMending(@NotNull Player player, int experience) {
        if (experience <= 0) {
            return 0;
        }

        ItemStack repairableItem = getRepairableItem(player);

        if (repairableItem == null) {
            return experience;
        }

        // Actually repair the item.
        ItemMeta meta = repairableItem.getItemMeta();

        if (meta == null) {
            return experience;
        }

        Damageable damageable = (Damageable) meta;

        int totalDurability = experience * 2;

        int min = Math.min(totalDurability, damageable.getDamage());

        int experienceUsed = (int) Math.ceil(min / 2.0);

        // PlayerItemMendEvent un-callable due to ExperienceOrb being required. We don't have one. Too much effort.

        damageable.setDamage(damageable.getDamage() - min);

        repairableItem.setItemMeta(meta);

        return applyMending(player, experience - experienceUsed);
    }

    @Override
    public @NotNull Item createDroppedItem(@NotNull Location location, @NotNull ItemStack item) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(location.getWorld(), "Location world not loaded.");

        Item entity = location.getWorld().createEntity(location, Item.class);
        entity.setItemStack(item);
        return entity;
    }
}
