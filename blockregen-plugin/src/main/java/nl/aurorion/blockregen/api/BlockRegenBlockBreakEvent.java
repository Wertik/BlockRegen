package nl.aurorion.blockregen.api;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.listener.EventType;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired after the original BlockBreakEvent.
 * Cancelling this event causes BlockRegen not to do any action after the block is broken. It does not cancel BlockBreakEvent itself.
 */
public class BlockRegenBlockBreakEvent extends BlockRegenBlockEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The event that caused BlockRegen triggering.
     */
    @Getter
    @NotNull
    private final Event event;

    @Getter
    @NotNull
    private final EventType eventType;

    @Getter
    @Nullable
    private final RegenerationArea area;

    @Getter
    @Setter
    private boolean cancelled = false;

    public BlockRegenBlockBreakEvent(@NotNull Block block, @NotNull BlockPreset blockPreset, @NotNull Event event, @NotNull EventType eventType, @Nullable RegenerationArea area) {
        super(block, blockPreset);
        this.event = event;
        this.eventType = eventType;
        this.area = area;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}