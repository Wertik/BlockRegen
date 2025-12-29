package nl.aurorion.blockregen.listener;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.regeneration.EventControl;
import nl.aurorion.blockregen.regeneration.RegenerationEventType;
import nl.aurorion.blockregen.util.BukkitVersions;
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

@Log
public class RegenerationListener implements Listener {

    private final BlockRegenPlugin plugin;

    public RegenerationListener(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    // Block trampling
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            // shouldn't happen with trampling
            return;
        }

        XMaterial xMaterial = plugin.getBlockType(block);
        if (xMaterial != XMaterial.FARMLAND) {
            return;
        }

        Player player = event.getPlayer();
        Block cropBlock = block.getRelative(BlockFace.UP);

        plugin.getRegenerationEventHandler().handleEvent(cropBlock, player, event, new EventControl<PlayerInteractEvent>() {
            @Override
            public void cancelDrops() {
                //
            }

            @Override
            public void cancel() {
                event.setCancelled(true);
            }

            @Override
            public int getDefaultExperience() {
                return 0;
            }
        }, RegenerationEventType.TRAMPLING);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        int defaultExperience = event.getExpToDrop();

        plugin.getRegenerationEventHandler().handleEvent(block, player, event, new EventControl<BlockBreakEvent>() {
            @Override
            public void cancelDrops() {
                // We're dropping the items ourselves.
                if (BukkitVersions.isCurrentAbove("1.8", false)) {
                    event.setDropItems(false);
                    log.fine(() -> "Cancelled BlockDropItemEvent");
                }

                event.setExpToDrop(0);
            }

            @Override
            public int getDefaultExperience() {
                return defaultExperience;
            }

            @Override
            public void cancel() {
                event.setCancelled(true);
            }
        }, RegenerationEventType.BLOCK_BREAK);
    }
}
