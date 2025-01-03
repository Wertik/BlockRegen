package nl.aurorion.blockregen.system.regeneration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.AutoSaveTask;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

@Log
public class RegenerationManager {

    private final BlockRegen plugin;

    private final ConcurrentLinkedDeque<RegenerationProcess> cache = new ConcurrentLinkedDeque<>();

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
    public RegenerationProcess createProcess(@NotNull Block block, @NotNull BlockPreset preset, @Nullable String regionName) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(preset);

        // Read the original material
        NodeData nodeData = plugin.getVersionManager().createNodeData();
        nodeData.load(block);

        RegenerationProcess process = new RegenerationProcess(block, nodeData, preset);

        process.setWorldName(block.getWorld().getName());
        process.setRegionName(regionName);

        return process;
    }

    /**
     * Register the process as running.
     */
    public void registerProcess(@NotNull RegenerationProcess process) {
        Objects.requireNonNull(process);

        if (this.getProcess(process.getBlock()) != null) {
            log.fine(() -> String.format("Cache already contains process %s", process.getId()));
            return;
        }

        cache.add(process);
        log.fine(() -> "Registered regeneration process " + process);
    }

    @Nullable
    public RegenerationProcess getProcess(@NotNull Block block) {
        for (RegenerationProcess process : cache) {
            // Try to convert simple location again and exit if the block's not there.
            if (process.getBlock() == null) {
                continue;
            }

            if (!process.getBlock().equals(block)) {
                continue;
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
            log.fine(() -> String.format("Removed process from cache: %s", process));
        } else {
            log.fine(() -> String.format("Process %s not found, not removed.", process));
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

    // Revert blocks before disabling
    public void revertAll() {
        cache.forEach(RegenerationProcess::revertBlock);
    }

    // Can only be called from the main thread
    private void purgeExpired() {
        // Clear invalid processes
        for (RegenerationProcess process : new HashSet<>(cache)) {
            if (process.getTimeLeft() < 0 && process.shouldRegenerate()) {
                if (Bukkit.isPrimaryThread()) {
                    process.regenerateBlock();
                } else {
                    Bukkit.getScheduler().runTask(plugin, process::regenerateBlock);
                }
            }
        }
    }

    public void save() {
        save(false);
    }

    public void save(boolean sync) {
        cache.forEach(process -> process.setTimeLeft(process.getRegenerationTime() - System.currentTimeMillis()));

        // TODO: Shouldn't be required
        purgeExpired();

        final List<RegenerationProcess> finalCache = new ArrayList<>(cache);

        CompletableFuture<Void> future = plugin.getGsonHelper().save(finalCache, plugin.getDataFolder().getPath() + "/Data.json")
                .exceptionally(e -> {
                    log.severe("Could not save processes: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });

        if (sync) {
            future.join();
        }

        log.info("Saved " + finalCache.size() + " regeneration processes..");
    }

    public void load() {
        plugin.getGsonHelper().loadListAsync(plugin.getDataFolder().getPath() + "/Data.json", RegenerationProcess.class)
                .thenAcceptAsync(loadedProcesses -> {
                    cache.clear();

                    if (loadedProcesses == null) {
                        return;
                    }

                    for (RegenerationProcess process : loadedProcesses) {
                        if (process == null) {
                            log.warning("Failed to load a process from storage. Report this to the maintainer of the plugin.");
                            continue;
                        }

                        if (!process.convertLocation()) {
                            this.retry = true;
                            break;
                        }

                        if (!process.convertPreset()) {
                            Bukkit.getScheduler().runTask(plugin, process::revert);
                            continue;
                        }
                        log.fine(() -> "Prepared regeneration process " + process);
                    }

                    if (!this.retry) {
                        // Start em
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            loadedProcesses.forEach(RegenerationProcess::start);
                            log.info("Loaded " + this.cache.size() + " regeneration process(es)...");
                        });
                    } else {
                        log.info(
                                "One of the worlds is probably not loaded. Loading after complete server load instead.");
                    }
                }).exceptionally(e -> {
                    log.severe("Could not load processes: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });
    }

    public void reattemptLoad() {
        if (!retry) {
            return;
        }

        load();

        // Override retry flag from this load.
        this.retry = false;
    }

    public Collection<RegenerationProcess> getCache() {
        return Collections.unmodifiableCollection(cache);
    }
}