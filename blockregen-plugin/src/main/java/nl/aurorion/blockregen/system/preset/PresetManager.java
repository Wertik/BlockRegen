package nl.aurorion.blockregen.system.preset;

import com.cryptomorin.xseries.XSound;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.PresetConditions;
import nl.aurorion.blockregen.system.preset.struct.PresetRewards;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log
public class PresetManager {

    private final BlockRegen plugin;

    private final Map<String, BlockPreset> presets = new HashMap<>();

    public PresetManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public BlockPreset getPreset(@Nullable String name) {
        return presets.get(name);
    }

    @Nullable
    public BlockPreset getPreset(@NotNull Block block) {
        for (BlockPreset preset : this.presets.values()) {
            if (preset.getTargetMaterial().matches(block)) {
                return preset;
            }
        }
        return null;
    }

    @Nullable
    public BlockPreset getPreset(@NotNull Block block, @Nullable RegenerationArea region) {
        if (region == null) {
            return getPreset(block);
        }

        for (BlockPreset preset : this.presets.values()) {
            if (preset.getTargetMaterial().matches(block) && region.hasPreset(preset.getName())) {
                return preset;
            }
        }
        return null;
    }

    public Map<String, BlockPreset> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public void load() {
        presets.clear();

        // Clear all events before loading.
        plugin.getEventManager().clearEvents();

        ConfigurationSection blocks = plugin.getFiles().getBlockList().getFileConfiguration()
                .getConfigurationSection("Blocks");

        if (blocks == null) {
            return;
        }

        for (String key : blocks.getKeys(false)) {
            try {
                load(key);
            } catch (Exception e) {
                log.warning(String.format("Could not load preset %s: %s", key, e.getMessage()));
            }
        }

        log.info("Loaded " + presets.size() + " block preset(s)...");
        log.info("Added " + plugin.getEventManager().getLoadedEvents().size() + " event(s)...");
    }

    public void load(String name) throws IllegalArgumentException {
        FileConfiguration file = plugin.getFiles().getBlockList().getFileConfiguration();

        ConfigurationSection section = file.getConfigurationSection("Blocks." + name);

        if (section == null) {
            return;
        }

        BlockPreset preset = new BlockPreset(name);

        String targetMaterialInput = section.getString("target-material", name);

        // Target material
        TargetMaterial targetMaterial = this.plugin.getMaterialManager().parseTargetMaterial(targetMaterialInput);
        preset.setTargetMaterial(targetMaterial);
        log.fine(() -> String.format("target-material: %s", preset.getTargetMaterial()));

        // Replace material
        String replaceMaterial = section.getString("replace-block");

        if (!Strings.isNullOrEmpty(replaceMaterial)) {
            try {
                preset.setReplaceMaterial(this.plugin.getMaterialManager().parsePlacementMaterial(replaceMaterial));
            } catch (IllegalArgumentException e) {
                log.warning("Dynamic material ( " + replaceMaterial + " ) in 'replace-block' for " + name + " is invalid: " + e.getMessage());
            }
        }
        log.fine(() -> String.format("replace-material: %s", preset.getReplaceMaterial()));

        // Regenerate into
        String regenerateIntoInput = section.getString("regenerate-into");

        if (!Strings.isNullOrEmpty(regenerateIntoInput)) {
            try {
                preset.setRegenMaterial(this.plugin.getMaterialManager().parsePlacementMaterial(regenerateIntoInput));
            } catch (IllegalArgumentException e) {
                log.warning("Dynamic material ( " + replaceMaterial + " ) in 'regenerate-into' for " + name + " is invalid: " + e.getMessage());
            }
        }
        log.fine(() -> String.format("regenerate-into: %s", preset.getRegenMaterial()));

        // Delay
        preset.setDelay(Amount.load(file, "Blocks." + name + ".regen-delay", 3));

        // Natural break
        preset.setNaturalBreak(section.getBoolean("natural-break", true));

        preset.setDisablePhysics(section.getBoolean("disable-physics", false));

        // Apply fortune
        preset.setApplyFortune(section.getBoolean("apply-fortune", true));

        // Drop naturally
        preset.setDropNaturally(section.getBoolean("drop-naturally", true));

        // Handle crops
        preset.setHandleCrops(section.getBoolean("handle-crops", true));

        // Solid ground
        preset.setCheckSolidGround(section.getBoolean("check-solid-ground", true));

        // Regenerate whole
        preset.setRegenerateWhole(section.getBoolean("regenerate-whole", false));

        // Block Break Sound
        String sound = section.getString("sound");

        if (!Strings.isNullOrEmpty(sound)) {
            Optional<XSound> xSound = XSound.of(sound);
            if (!xSound.isPresent()) {
                log.warning("Sound '" + sound + "' in preset " + name + " is invalid.");
            } else {
                preset.setSound(xSound.get());
            }
        }

        // Particle
        String particleName = section.getString("particles");
        if (!Strings.isNullOrEmpty(particleName)) {
            preset.setParticle(particleName);
        }

        String regenParticle = section.getString("regeneration-particles");
        if (!Strings.isNullOrEmpty(regenParticle)) {
            preset.setRegenerationParticle(regenParticle);
        }

        // Conditions
        PresetConditions conditions = new PresetConditions();

        // Tools
        String toolsRequired = section.getString("tool-required");
        if (!Strings.isNullOrEmpty(toolsRequired)) {
            conditions.setToolsRequired(toolsRequired);
        }

        // Enchants
        String enchantsRequired = section.getString("enchant-required");
        if (!Strings.isNullOrEmpty(enchantsRequired)) {
            conditions.setEnchantsRequired(enchantsRequired);
        }

        // Jobs
        if (plugin.getCompatibilityManager().getJobs().isLoaded()) {
            String jobsRequired = section.getString("jobs-check");
            if (!Strings.isNullOrEmpty(jobsRequired)) {
                conditions.setJobsRequired(jobsRequired);
            }
        }

        preset.setConditions(conditions);

        // Rewards
        PresetRewards rewards = PresetRewards.load(section, preset);

        preset.setRewards(rewards);

        PresetEvent event = PresetEvent.load(section.getConfigurationSection("event"), name, preset);

        if (event != null) {
            plugin.getEventManager().addEvent(event);
        }

        presets.put(name, preset);
        log.fine(() -> "Loaded preset " + preset);
    }
}