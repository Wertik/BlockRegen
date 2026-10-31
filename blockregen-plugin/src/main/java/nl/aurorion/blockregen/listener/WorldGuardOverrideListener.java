package nl.aurorion.blockregen.listener;

import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Allow players to break blocks with a preset inside regions with WorldGuard-Override enabled,
 * even when WorldGuard denies breaking there. Trampling a crop with a preset is allowed the same way.
 */
@Log
public class WorldGuardOverrideListener implements Listener {

    private final BlockRegenPlugin plugin;

    public WorldGuardOverrideListener(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    // Before WorldGuard checks the event. WorldGuard skips events that are already allowed.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getCause().getRootCause() instanceof Player) || event.getBlocks().isEmpty()) {
            return;
        }

        // WorldGuard turns trampling into a break of the farmland, the crop with the preset is above it.
        if (isTrampling(event.getOriginalEvent())) {
            for (Block block : event.getBlocks()) {
                if (!plugin.getRegionManager().shouldOverrideWorldGuard(block.getRelative(BlockFace.UP))) {
                    return;
                }
            }
            event.setResult(Event.Result.ALLOW);
            log.fine(() -> "Allowed WorldGuard trampling for configured crop.");
            return;
        }

        // Only a player breaking a block by hand, not explosions, pistons etc.
        if (!(event.getOriginalEvent() instanceof BlockBreakEvent)) {
            return;
        }

        for (Block block : event.getBlocks()) {
            if (!plugin.getRegionManager().shouldOverrideWorldGuard(block)) {
                return;
            }
        }

        event.setResult(Event.Result.ALLOW);
        log.fine(() -> "Allowed WorldGuard block break for configured block.");
    }

    // After the break, WorldGuard also checks placing dirt where the trampled farmland was.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlaceBlock(PlaceBlockEvent event) {
        if (!isTrampling(event.getOriginalEvent()) || !(event.getCause().getRootCause() instanceof Player)
                || event.getBlocks().isEmpty()) {
            return;
        }

        for (Block block : event.getBlocks()) {
            if (!plugin.getRegionManager().shouldOverrideWorldGuard(block.getRelative(BlockFace.UP))) {
                return;
            }
        }

        event.setResult(Event.Result.ALLOW);
        log.fine(() -> "Allowed WorldGuard trampling for configured crop.");
    }

    private static boolean isTrampling(Event originalEvent) {
        return originalEvent instanceof PlayerInteractEvent
                && ((PlayerInteractEvent) originalEvent).getAction() == Action.PHYSICAL;
    }
}
