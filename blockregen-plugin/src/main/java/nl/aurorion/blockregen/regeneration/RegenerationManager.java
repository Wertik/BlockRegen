package nl.aurorion.blockregen.regeneration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.AutoSaveTask;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Log
public class RegenerationManager {

    private final BlockRegenPlugin plugin;

    private final Map<Block, RegenerationProcess> cache = new ConcurrentHashMap<>();

    @Getter
    private AutoSaveTask autoSaveTask;

    private boolean retry = false;

    private final Set<UUID> bypass = new HashSet<>();

    private final Set<UUID> dataCheck = new HashSet<>();

    public RegenerationManager(BlockRegenPlugin plugin) {
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

    @NotNull
    public RegenerationProcess createProcess(@NotNull Block block, @NotNull BlockRegenMaterial originalMaterial, @NotNull BlockPreset preset, @Nullable RegenerationArea area) {
        RegenerationProcess process = new RegenerationProcess(block, preset, originalMaterial);

        process.setWorldName(block.getWorld().getName());
        if (area != null) {
            process.setRegionName(area.getName());
        }
        return process;
    }

    /**
     * Helper for creating regeneration processes.
     */
    @NotNull
    public RegenerationProcess createProcess(@NotNull Block block, @NotNull BlockPreset preset, @Nullable RegenerationArea region) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(preset);

        BlockRegenMaterial material = plugin.getMaterialManager().getMaterial(block);

        if (material == null) {
            // todo: well what now, the preset probably already matched?
            throw new IllegalStateException("Shouldn't return null...");
        }

        RegenerationProcess process = new RegenerationProcess(block, preset, material);

        process.setWorldName(block.getWorld().getName());
        if (region != null) {
            process.setRegionName(region.getName());
        }
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

        cache.put(process.getBlock(), process);
        log.fine(() -> "Registered regeneration process " + process);
    }

    @Nullable
    public RegenerationProcess getProcess(@NotNull Block block) {
        return this.cache.get(block);
    }

    public boolean isRegenerating(@NotNull Block block) {
        RegenerationProcess process = getProcess(block);
        return process != null && process.getRegenerationTime() > System.currentTimeMillis();
    }

    public void removeProcess(RegenerationProcess process) {
        if (cache.remove(process.getBlock()) != null) {
            log.fine(() -> String.format("Removed process from cache: %s", process));
        } else {
            log.fine(() -> String.format("Process %s not found, not removed.", process));
        }
    }

    public void removeProcess(@NotNull Block block) {
        cache.remove(block);
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
        cache.values().forEach(RegenerationProcess::revertBlock);
    }

    // Can only be called from the main thread
    private void purgeExpired() {
        // Clear invalid processes
        for (RegenerationProcess process : cache.values()) {
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
        final File dataFile = new File(plugin.getDataFolder(), "/Data.json");

        if (cache.isEmpty()) {
            log.fine(() -> "No processes to save.");
            try {
                Files.write(dataFile.toPath(), "[]\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                log.severe(() -> "Failed to create empty Data.json.");

                // Try to force delete.
                //noinspection ResultOfMethodCallIgnored
                dataFile.delete();
            }
            return;
        }

        cache.values().forEach(process -> process.setTimeLeft(process.getRegenerationTime() - System.currentTimeMillis()));

        // TODO: Shouldn't be required
        purgeExpired();

        final List<RegenerationProcess> finalCache = new ArrayList<>(cache.values());

        CompletableFuture<Void> future = plugin.getGsonHelper().save(finalCache, dataFile.toPath())
                .exceptionally(e -> {
                    log.log(Level.SEVERE, "Could not save processes: " + e.getMessage(), e);
                    return null;
                });

        if (sync) {
            future.join();
        }

        log.fine(() -> "Saved " + finalCache.size() + " regeneration processes..");
    }

    public void load() {
        plugin.getGsonHelper().loadListAsync(plugin.getDataFolder().getPath() + "/Data.json", RegenerationProcess.class)
                .thenAcceptAsync(loadedProcesses ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
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
                                    log.warning("Failed to prepare process '" + process.getPresetName() + "'.");
                                    break;
                                }

                                if (!process.convertPreset()) {
                                    this.retry = true;
                                    log.warning("Failed to prepare process '" + process.getId() + "'.");
                                    break;
                                }
                                log.fine(() -> "Prepared regeneration process " + process);
                            }

                            if (!this.retry) {
                                // Start em
                                loadedProcesses.forEach(RegenerationProcess::start);
                                log.info("Loaded " + this.cache.size() + " regeneration process(es)...");
                            } else {
                                log.info("Some processes couldn't load, trying again after a complete server load.");
                            }
                        })).exceptionally(e -> {
                    log.log(Level.SEVERE, "Could not load processes: " + e.getMessage(), e);
                    return null;
                });
    }

    public void reattemptLoad() {
        if (!retry) {
            return;
        }

        load();

        this.retry = false;
    }

    @NotNull
    public Collection<RegenerationProcess> getCache() {
        return Collections.unmodifiableCollection(cache.values());
    }
}