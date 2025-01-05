package nl.aurorion.blockregen.system.event;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
public class EventManager {

    private final BlockRegen plugin;

    private final Map<String, PresetEvent> loadedEvents = new HashMap<>();

    public EventManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void clearBars() {
        for (PresetEvent event : getEvents(e -> e.isEnabled() && e.getActiveBossBar() != null)) {
            event.getActiveBossBar().removeAll();
        }
    }

    /**
     * Remove Player from all active bars.
     */
    public void removeBars(Player player) {
        if (plugin.getVersionManager().isCurrentBelow("1.8", true))
            return;

        for (PresetEvent event : getEvents(e -> e.isEnabled() && e.getActiveBossBar() != null)) {
            event.getActiveBossBar().addPlayer(player);
        }
    }

    /**
     * Add player to all active bars.
     */
    public void addBars(Player player) {
        if (plugin.getVersionManager().isCurrentBelow("1.8", true))
            return;

        for (PresetEvent event : getEvents(e -> e.isEnabled() && e.getActiveBossBar() != null)) {
            event.getActiveBossBar().addPlayer(player);
        }
    }

    public void disableEvent(PresetEvent event) {
        if (!event.isEnabled())
            return;

        event.setEnabled(false);
        log.fine(() -> "Disabled event " + event.getName());

        if (plugin.getVersionManager().isCurrentBelow("1.8", true))
            return;

        // Boss bar
        BossBar bossBar = event.getActiveBossBar();

        if (bossBar == null)
            return;

        bossBar.removeAll();
    }

    public void disableEvent(String name) {
        PresetEvent event = this.loadedEvents.get(name);

        if (event != null)
            disableEvent(event);
    }

    public void disableAll() {
        getEvents(PresetEvent::isEnabled).forEach(this::disableEvent);
    }

    public void enableEvent(@NotNull PresetEvent event) {

        if (event.isEnabled())
            return;

        event.setEnabled(true);
        log.fine(() -> "Enabled event " + event.getName());

        if (plugin.getVersionManager().isCurrentBelow("1.8", true) || event.getBossBar() == null)
            return;

        // Boss bar
        BossBar bossBar = plugin.getVersionManager().getMethods().createBossBar(event.getBossBar().getText(),
                event.getBossBar().getColor(), event.getBossBar().getStyle());
        if (bossBar == null)
            return;

        event.setActiveBossBar(bossBar);

        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    public void enableEvent(String name) {
        PresetEvent event = this.loadedEvents.get(name);
        if (event != null)
            enableEvent(event);
    }

    public void addEvent(PresetEvent event) {
        this.loadedEvents.put(event.getName(), event);
        log.fine(() -> "Added event " + event);
    }

    public boolean isEnabled(String name) {
        return this.loadedEvents.containsKey(name) && this.loadedEvents.get(name).isEnabled();
    }

    @Nullable
    public PresetEvent getEvent(String name) {
        return this.loadedEvents.get(name);
    }

    public void clearEvents() {
        this.loadedEvents.clear();
    }

    public Set<PresetEvent> getEvents(Predicate<PresetEvent> predicate) {
        return getLoadedEvents().values().stream()
                .filter(predicate)
                .collect(Collectors.toSet());
    }

    public Map<String, PresetEvent> getLoadedEvents() {
        return Collections.unmodifiableMap(loadedEvents);
    }
}
