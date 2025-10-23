package nl.aurorion.blockregen.regeneration;

import org.bukkit.event.Event;

/**
 * Allows the RegenerationEventHandler to operate on the event.
 *
 * @param <E>
 */
public interface EventControl<E extends Event> {
    /**
     * Disable/clear drops on the event.
     */
    void cancelDrops();

    /**
     * Cancel the event.
     */
    void cancel();

    int getDefaultExperience();
}
