package nl.aurorion.blockregen.system.regeneration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.AutoSaveTask;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Log
public class RegenerationManager {

    private final BlockRegen plugin;

    private final List<RegenerationProcess> cache = new ArrayList<>();

    @Getter
    private AutoSaveTask autoSaveTask;

    private boolean retry = false;

    private final Set<UUID> bypass = new HashSet<>();

    private final Set<UUID> dataCheck = new HashSet<>();

    public RegenerationManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    // --- Bypass

    public boolean hasBypass(@NotNull Player player) {
        return bypass.contains(player.getUniqueId());
    }

    /**
     * Switch the bypass status of the player. Return the state after the change.
     */
    public boolean switchBypass(@NotNull Player player) {
        if (bypass.contains(player.getUniqueId())) {
            bypass.remove(player.getUniqueId());
            return false;
        } else {
            bypass.add(player.getUniqueId());
            return true;
        }
    }

    // --- Data Check

    public boolean hasDataCheck(@NotNull Player player) {
        return dataCheck.contains(player.getUniqueId());
    }

    public boolean switchDataCheck(@NotNull Player player) {
        if (dataCheck.contains(player.getUniqueId())) {
            dataCheck.remove(player.getUniqueId());
            return false;
        } else {
            dataCheck.add(player.getUniqueId());
            return true;
        }
    }

    /**
     * Helper for creating regeneration processes.
     */
    @NotNull
    public RegenerationProcess createProcess(@NotNull Block block, @NotNull BlockPreset preset, String... regionName) {
        RegenerationProcess process = createProcess(block, preset);

        process.setWorldName(block.getWorld().getName());

        if (regionName.length > 0) {
            process.setRegionName(regionName[0]);
        }

        return process;
    }

    /**
     * Helper for creating regeneration processes.
     */
    @NotNull
    public RegenerationProcess createProcess(@NotNull Block block, @NotNull BlockPreset preset) {

        Objects.requireNonNull(block);
        Objects.requireNonNull(preset);

        // Read the original material
        NodeData nodeData = plugin.getVersionManager().createNodeData();
        nodeData.load(block);

        return new RegenerationProcess(block, nodeData, preset);
    }

    /**
     * Register the process as running.
     */
    public void registerProcess(@NotNull RegenerationProcess process) {
        if (cache.contains(process)) {
            log.fine(String.format("Cache already contains process for location %s", process.getLocation()));
            return;
        }

        cache.add(process);
        log.fine("Registered regeneration process " + process);
    }

    @Nullable
    public RegenerationProcess getProcess(@NotNull Block block) {

        Location location = block.getLocation();

        for (RegenerationProcess process : new HashSet<>(getCache())) {

            // Don't know why I need to do this.
            if (process == null)
                continue;

            // Try to convert simple location again and exit if the block's not there.
            if (process.getBlock() == null)
                continue;

            if (!process.getBlock().getLocation().equals(location))
                continue;

            // Try to start the process again.
            if (process.getTimeLeft() < 0 && !process.start()) {
                return null;
            }

            return process;
        }
        return null;
    }

    public boolean isRegenerating(@NotNull Block block) {
        RegenerationProcess process = getProcess(block);

        return process != null && process.getRegenerationTime() > System.currentTimeMillis();
    }

    public void removeProcess(RegenerationProcess process) {
        if (cache.remove(process)) {
            log.fine(String.format("Removed process from cache: %s", process));
        } else {
            log.fine(String.format("Process %s not found, not removed.", process));
        }
    }

    public void removeProcess(@NotNull Block block) {
        cache.removeIf(process -> process.getBlock().equals(block));
    }

    public void startAutoSave() {
        this.autoSaveTask = new AutoSaveTask(plugin);

        autoSaveTask.load();
        autoSaveTask.start();
    }

    public void reloadAutoSave() {
        if (autoSaveTask == null) {
            startAutoSave();
        } else {
            autoSaveTask.stop();
            autoSaveTask.load();
            autoSaveTask.start();
        }
    }

    public void revertAll() {
        revertAll(true);
    }

    // Revert blocks before disabling
    public void revertAll(boolean synchronize) {
        cache.forEach(process -> process.revertBlock(synchronize));
    }

    private void purgeExpired() {

        // Clear invalid processes
        for (RegenerationProcess process : new HashSet<>(cache)) {

            if (process == null)
                continue;

            if (process.getTimeLeft() < 0)
                process.regenerateBlock();
        }
    }

    public void save() {
        save(false);
    }

    public void save(boolean sync) {
        cache.forEach(process -> {
            if (process != null)
                process.setTimeLeft(process.getRegenerationTime() - System.currentTimeMillis());
        });

        purgeExpired();

        final List<RegenerationProcess> finalCache = new ArrayList<>(cache);

        log.fine("Saving " + finalCache.size() + " regeneration processes..");

        CompletableFuture<Void> future = plugin.getGsonHelper().save(finalCache, plugin.getDataFolder().getPath() + "/Data.json")
                .exceptionally(e -> {
                    log.severe("Could not save processes: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });

        if (sync) {
            future.join();
        }
    }

    public void load() {
        plugin.getGsonHelper().loadListAsync(plugin.getDataFolder().getPath() + "/Data.json", RegenerationProcess.class)
                .thenAcceptAsync(loadedProcesses -> {
                    cache.clear();

                    if (loadedProcesses == null)
                        loadedProcesses = new ArrayList<>();

                    for (RegenerationProcess process : loadedProcesses) {

                        if (!process.convertLocation()) {
                            this.retry = true;
                            break;
                        }

                        if (!process.convertPreset()) {
                            process.revert();
                            continue;
                        }
                        log.fine("Prepared regeneration process " + process);
                    }

                    if (!this.retry) {
                        // Start em
                        loadedProcesses.forEach(RegenerationProcess::start);
                        log.info("Loaded " + this.cache.size() + " regeneration process(es)...");
                    } else
                        log.info(
                                "One of the worlds is probably not loaded. Loading after complete server load instead.");
                }).exceptionally(e -> {
                    log.severe("Could not load processes: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });
    }

    public void reattemptLoad() {
        if (!retry)
            return;

        load();
        // Override retry flag from this load.
        this.retry = false;
    }

    public List<RegenerationProcess> getCache() {
        return Collections.unmodifiableList(cache);
    }
}