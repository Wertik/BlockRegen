package nl.aurorion.blockregen.regeneration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface RegenerationEventHandler {
    <E extends Event> void handleEvent(Block block, Player player, E event, EventControl<E> eventControl, RegenerationEventType type);
}
