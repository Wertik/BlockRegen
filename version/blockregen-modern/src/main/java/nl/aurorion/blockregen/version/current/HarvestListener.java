package nl.aurorion.blockregen.version.current;

import nl.aurorion.blockregen.regeneration.EventControl;
import nl.aurorion.blockregen.regeneration.RegenerationEventType;
import nl.aurorion.blockregen.regeneration.RegenerationEventHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

public class HarvestListener implements Listener {
    private final RegenerationEventHandler eventHandler;

    public HarvestListener(RegenerationEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Block block = event.getHarvestedBlock();
        Player player = event.getPlayer();

        eventHandler.handleEvent(block, player, event, new EventControl<PlayerHarvestBlockEvent>() {
            @Override
            public void cancelDrops() {
                event.getItemsHarvested().clear();
            }

            @Override
            public void cancel() {
                event.setCancelled(true);
            }

            @Override
            public int getDefaultExperience() {
                return 0;
            }
        }, RegenerationEventType.HARVEST);
    }
}
