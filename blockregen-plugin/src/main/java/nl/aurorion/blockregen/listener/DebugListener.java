package nl.aurorion.blockregen.listener;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAfterBreak(BlockBreakEvent event) {
        log.fine(String.format("After break: %s, %s", event.isCancelled(), event.getBlock().getType()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAfterDrop(BlockDropItemEvent event) {
        log.fine(String.format("After drop: %s, %s [%s]", event.isCancelled(), event.getBlockState().getType(), event.getItems().stream().map(
                (i) -> i.getItemStack().getType() + "x" + i.getItemStack().getAmount()
        ).collect(Collectors.joining(","))));
    }
}
