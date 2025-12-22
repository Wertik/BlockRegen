package nl.aurorion.blockregen.listener;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.util.Versions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Log information about events.
 */
@Log
public class DebugListener implements Listener {

    private final BlockRegenPlugin plugin;

    @Getter
    @Setter
    private boolean registered = false;

    public DebugListener(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    private static void suppressThrows(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.log(Level.FINE, "DebugListener threw an exception: " + e.getMessage(), e);
        }
    }

    public void register() {
        setRegistered(true);

        this.plugin.getServer().getPluginManager().registerEvent(BlockBreakEvent.class, this, EventPriority.MONITOR, (listener, event) -> {
            suppressThrows(() -> {
                if (!(event instanceof BlockBreakEvent)) {
                    return;
                }
                BlockBreakEvent blockBreakEvent = (BlockBreakEvent) event;
                log.fine(String.format("BlockBreakEvent: %s, %s", blockBreakEvent.isCancelled(), blockBreakEvent.getBlock().getType()));
            });
        }, this.plugin);

        this.plugin.getServer().getPluginManager().registerEvent(EntitySpawnEvent.class, this, EventPriority.MONITOR, (listener, event) -> {
            suppressThrows(() -> {
                if (!(event instanceof EntitySpawnEvent)) {
                    return;
                }
                EntitySpawnEvent entitySpawnEvent = (EntitySpawnEvent) event;

                Entity entity = entitySpawnEvent.getEntity();

                if (!(entity instanceof Item)) {
                    return;
                }

                Item item = ((Item) entity);
                ItemStack itemStack = item.getItemStack();

                log.fine(String.format("EntitySpawnEvent: %s, %sx%d", entitySpawnEvent.isCancelled(), itemStack.getType(), itemStack.getAmount()));
            });
        }, this.plugin);

        // BlockDropItemEvent added in 1.13
        if (Versions.isCurrentAbove("1.13", true)) {
            this.plugin.getServer().getPluginManager().registerEvent(BlockDropItemEvent.class, this, EventPriority.MONITOR, (listener, event) -> {
                suppressThrows(() -> {
                    if (!(event instanceof BlockDropItemEvent)) {
                        return;
                    }
                    BlockDropItemEvent blockDropItemEvent = (BlockDropItemEvent) event;
                    log.fine(String.format("BlockDropItemEvent: %s, %s [%s]", blockDropItemEvent.isCancelled(), blockDropItemEvent.getBlockState().getType(), blockDropItemEvent.getItems().stream().map(
                            (i) -> i.getItemStack().getType() + "x" + i.getItemStack().getAmount()
                    ).collect(Collectors.joining(","))));
                });
            }, this.plugin);
        }
    }

    public void unregister() {
        this.setRegistered(false);
        HandlerList.unregisterAll(this);
        log.fine(() -> "Unregistered debug listener.");
    }
}
