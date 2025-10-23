package nl.aurorion.blockregen.regeneration.struct;

import com.cryptomorin.xseries.XMaterial;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.api.BlockRegenBlockRegenerationEvent;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MinecraftMaterial;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.preset.FixedNumberValue;
import nl.aurorion.blockregen.util.Blocks;
import nl.aurorion.blockregen.util.Locations;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Log
@Data
public class RegenerationProcess {

    private final UUID id = UUID.randomUUID();

    private SimpleLocation location;

    private transient Block block;

    private XMaterial originalMaterial;
    @Getter
    private NodeData originalData;

    @Getter
    private String regionName;
    @Getter
    private String worldName;

    private String presetName;

    @Getter
    private transient BlockPreset preset;

    /*
     * Holds the system time when the block should regenerate.
     * -- is set after #start()
     */
    @Getter
    private transient long regenerationTime;

    private transient BlockRegenMaterial replaceMaterial;

    @Getter
    private long timeLeft = -1;

    @Setter
    private transient BlockRegenMaterial regenerateInto;

    private transient BukkitTask task;

    public RegenerationProcess(Block block, NodeData originalData, BlockPreset preset) {
        this.block = block;
        this.location = new SimpleLocation(block);

        this.preset = preset;
        this.presetName = preset.getName();

        this.worldName = block.getWorld().getName();

        this.originalData = originalData;
        this.originalMaterial = BlockRegenPlugin.getInstance().getVersionManager().getMethods().getType(block);
    }

    // Return true if the process started, false otherwise.
    public boolean start() {

        // Ensure to stop and null anything that ran before.
        stop();

        BlockRegenPlugin plugin = BlockRegenPluginImpl.getInstance();

        // Register that the process is actually running now
        // #start() can be called even on a process already in cache due to #contains() checks (which use #equals()) in RegenerationManager.
        plugin.getRegenerationManager().registerProcess(this);

        if (shouldRegenerate()) {
            // If timeLeft is -1, generate a new one from preset regen delay.
            if (timeLeft == -1) {
                int regenDelay = preset.getDelay().getInt();
                this.timeLeft = regenDelay * 1000L;
            }

            this.regenerationTime = System.currentTimeMillis() + timeLeft;

            // No need to start a task when it's time to regenerate already.
            if (timeLeft == 0 || regenerationTime <= System.currentTimeMillis()) {
                Bukkit.getScheduler().runTask(plugin, this::regenerate);
                log.fine(() -> "Regenerated the process upon start.");
                return false;
            }
        }

        Bukkit.getScheduler().runTask(plugin, this::replaceBlock);

        // No regeneration will be happening. Don't start the task.
        if (!shouldRegenerate()) {
            return true;
        }

        startTask();
        return true;
    }

    // <0 => don't regenerate. wait for manual regeneration.
    public boolean shouldRegenerate() {
        return !(preset.getDelay() instanceof FixedNumberValue && preset.getDelay().getInt() < 0);
    }

    private void startTask() {
        // Start the task
        this.task = Bukkit.getScheduler().runTaskLater(BlockRegenPluginImpl.getInstance(), this::regenerate, timeLeft / 50);
        log.fine(() -> String.format("Regenerate %s in %ds", this, timeLeft / 1000));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            this.task = null;
        }
    }

    /**
     * Regenerate the process and block.
     * <p>
     * Calls BlockRegenBlockRegenerationEvent.
     */
    public void regenerate() {
        log.fine(() -> "Regenerating " + this + "...");

        // Cancel the task if running.
        if (task != null) {
            task.cancel();
        }

        BlockRegenPlugin plugin = BlockRegenPluginImpl.getInstance();

        // If this block requires a block under it, wait for it to be there,
        // only if there's a running process at the block directly under.
        //
        // Otherwise, throw this process away.

        BlockRegenMaterial regenerateInto = getRegenerateInto();

        if (regenerateInto.requiresSolidGround() && preset.isCheckSolidGround()) {
            Block below = this.block.getRelative(BlockFace.DOWN);
            XMaterial belowType = plugin.getVersionManager().getMethods().getType(below);
            RegenerationProcess processBelow = plugin.getRegenerationManager().getProcess(below);

            // Sugarcane on sugarcane (aka not solid, still can be placed)
            // + kelp on kelp
            if (!below.getType().isSolid() && belowType != XMaterial.SUGAR_CANE && !Blocks.isKelp(belowType) && !Blocks.isSeagrass(belowType)) {
                if (processBelow != null) {
                    long delay = processBelow.getRegenerationTime() >= this.getRegenerationTime() ? processBelow.getRegenerationTime() - this.getRegenerationTime() + 100 : 1000;

                    // Regenerate with the block below.
                    this.timeLeft = delay;
                    this.regenerationTime = System.currentTimeMillis() + timeLeft;

                    log.fine(() -> "Delaying " + this + " to wait for " + processBelow + " delay: " + delay);

                    startTask();
                } else {
                    // no block under, no regeneration,... no hope
                    log.fine(() -> "No block under " + this + ", no point regenerating.");
                    plugin.getRegenerationManager().removeProcess(this);
                }
                return;
            }
        }

        // Call the event
        BlockRegenBlockRegenerationEvent blockRegenBlockRegenEvent = new BlockRegenBlockRegenerationEvent(this);
        Bukkit.getPluginManager().callEvent(blockRegenBlockRegenEvent);

        plugin.getRegenerationManager().removeProcess(this);

        if (blockRegenBlockRegenEvent.isCancelled()) {
            return;
        }

        regenerateBlock();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (preset.getRegenerationParticle() != null) {
                plugin.getParticleManager().displayParticle(preset.getRegenerationParticle(), block);
            }
        });

        this.task = null;
    }

    /**
     * Simply regenerate the block.
     */
    public void regenerateBlock() {
        // Set type
        BlockRegenMaterial regenerateInto = getRegenerateInto();

        // -- Regenerate farmland under crops
        if (regenerateInto.requiresFarmland()) {
            Block under = block.getRelative(BlockFace.DOWN);
            XMaterial underType = BlockRegenPluginImpl.getInstance().getVersionManager().getMethods().getType(under);

            if (underType != XMaterial.FARMLAND) {
                under.setType(Objects.requireNonNull(XMaterial.FARMLAND.get()));
            }
        }

        regenerateInto.setType(block);
        if (regenerateInto.applyOriginalData()) {
            originalData.apply(block);
        }
        regenerateInto.applyData(block); // Override with configured data if any
        log.fine(() -> "Regenerated " + this);
    }

    // Revert process to original material.
    public void revert() {
        stop();

        BlockRegenPlugin plugin = BlockRegenPluginImpl.getInstance();
        plugin.getRegenerationManager().removeProcess(this);

        revertBlock();
    }

    // Revert block to original state
    public void revertBlock() {
        BlockRegenMaterial original = getOriginalMaterial();

        // -- Place farmland under crops
        if (Blocks.requiresFarmland(originalMaterial)) {
            Block under = block.getRelative(BlockFace.DOWN);
            XMaterial underType = BlockRegenPluginImpl.getInstance().getVersionManager().getMethods().getType(under);
            if (underType != XMaterial.FARMLAND) {
                under.setType(Objects.requireNonNull(XMaterial.FARMLAND.get()));
            }
        }

        original.place(block);
        log.fine(() -> String.format("Reverted block for %s", this));
    }

    // Has to be synchronized to run on the next tick. Otherwise, the block does not get replaced.
    public void replaceBlock() {
        BlockRegenMaterial replaceMaterial = getReplaceMaterial();

        // -- Place farmland under crops
        if (replaceMaterial.requiresFarmland()) {
            Block under = block.getRelative(BlockFace.DOWN);
            XMaterial underType = BlockRegenPluginImpl.getInstance().getVersionManager().getMethods().getType(under);
            if (underType != XMaterial.FARMLAND) {
                under.setType(Objects.requireNonNull(XMaterial.FARMLAND.get()));
            }
        }

        replaceMaterial.setType(block);
        if (replaceMaterial.applyOriginalData()) {
            this.originalData.apply(block);
        }
        replaceMaterial.applyData(block); // Apply configured data if any

        // Otherwise skull textures wouldn't update.
        Bukkit.getScheduler().runTaskLater(BlockRegenPluginImpl.getInstance(), () -> block.getState().update(true), 1L);
        log.fine(() -> "Replaced block for " + this);
    }

    @NotNull
    public BlockRegenMaterial getRegenerateInto() {
        // Make sure we always get something.
        if (regenerateInto == null) {
            if (preset.getRegenMaterial() == null) {
                // todo: this breaks custom blocks... they regenerate into a minecraft material instead of the custom one.
                // todo: instead, save the block regen material and place it here.
                this.regenerateInto = new MinecraftMaterial(BlockRegenPlugin.getInstance(), originalMaterial, originalData);
            } else {
                this.regenerateInto = preset.getRegenMaterial().get();
            }
        }
        return regenerateInto;
    }

    @NotNull
    public BlockRegenMaterial getReplaceMaterial() {
        // Make sure we always get something.
        if (replaceMaterial == null) {
            if (preset.getReplaceMaterial() == null) {
                this.replaceMaterial = new MinecraftMaterial(BlockRegenPluginImpl.getInstance(), XMaterial.AIR, null);
            } else {
                this.replaceMaterial = preset.getReplaceMaterial().get();
            }
        }
        return replaceMaterial;
    }

    @NotNull
    public BlockRegenMaterial getOriginalMaterial() {
        return new MinecraftMaterial(BlockRegenPluginImpl.getInstance(), this.originalMaterial, this.originalData);
    }

    // Convert stored Location pointer to the Block at the location.
    public boolean convertLocation() {

        if (location == null) {
            log.severe("Could not load location for process " + this);
            return false;
        }

        Block block = this.location.toBlock();

        if (block == null) {
            log.severe("Could not load location for process " + this + ", world is invalid or not loaded.");
            return false;
        }

        // Prevent async chunk load.
        Bukkit.getScheduler().runTask(BlockRegenPluginImpl.getInstance(), () -> this.block = block);
        return true;
    }

    public boolean convertPreset() {
        BlockRegenPlugin plugin = BlockRegenPluginImpl.getInstance();

        BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

        if (preset == null) {
            log.severe("Could not load process " + this + ", it's preset '" + presetName + "' is invalid.");
            return false;
        }

        this.preset = preset;
        return true;
    }

    public void updateTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
        if (timeLeft > 0) {
            start();
        } else if (timeLeft == 0) {
            regenerate();
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    public Block getBlock() {
        if (this.block == null) {
            convertLocation();
        }
        return block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegenerationProcess process = (RegenerationProcess) o;
        return process.getLocation().equals(this.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.location);
    }

    @Override
    public String toString() {
        return String.format("{id=%s; task=%s; presetName=%s; worldName=%s; regionName=%s; block=%s; originalData=%s; originalMaterial=%s; regenerateInto=%s; replaceMaterial=%s; timeLeft=%d; regenerationTime=%d}",
                id,
                task == null ? "null" : task.getTaskId(),
                presetName,
                worldName,
                regionName,
                block == null ? "null" : Locations.locationToString(block.getLocation()),
                originalData,
                originalMaterial,
                this.regenerateInto,
                this.replaceMaterial,
                timeLeft,
                regenerationTime);
    }
}