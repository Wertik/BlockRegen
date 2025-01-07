package nl.aurorion.blockregen.region;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.region.struct.RawRegion;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import nl.aurorion.blockregen.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.region.struct.RegenerationWorld;
import nl.aurorion.blockregen.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Log
public class RegionManager {

    private final BlockRegenPlugin plugin;

    private final List<RegenerationArea> loadedAreas = new ArrayList<>();

    // Set of regions that failed to load.
    private final Set<RawRegion> failedRegions = new HashSet<>();

    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public RegionManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public void sort() {
        loadedAreas.sort((o1, o2) -> Comparator.comparing(RegenerationArea::getPriority).reversed().compare(o1, o2));
    }

    // ---- Selection

    public boolean isSelecting(@NotNull Player player) {
        return selections.containsKey(player.getUniqueId());
    }

    public RegionSelection getSelection(@NotNull Player player) {
        return selections.get(player.getUniqueId());
    }

    @NotNull
    public RegionSelection getOrCreateSelection(@NotNull Player player) {
        RegionSelection selection = selections.get(player.getUniqueId());

        if (selection == null) {
            selection = new RegionSelection();

            selections.put(player.getUniqueId(), selection);
        }

        return selection;
    }

    @NotNull
    public RegenerationRegion createRegion(@NotNull String name, @NotNull RegionSelection selection) {
        Location first = selection.getFirst();
        Location second = selection.getSecond();

        if (first.getWorld() != second.getWorld()) {
            throw new IllegalStateException("Selection points have to be in the same world.");
        }

        // Find out min and max.

        Location min = new Location(first.getWorld(), Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        Location max = new Location(first.getWorld(), Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));

        return new RegenerationRegion(name, min, max);
    }

    public boolean finishSelection(@NotNull String name, @NotNull RegionSelection selection) {
        RegenerationRegion region = createRegion(name, selection);
        addArea(region);
        return true;
    }

    @NotNull
    public RegenerationWorld createWorld(@NotNull String name, String worldName) {
        return new RegenerationWorld(name, worldName);
    }

    public void reattemptLoad() {
        if (failedRegions.isEmpty()) {
            return;
        }

        log.info("Reattempting to load regions...");
        int count = failedRegions.size();
        failedRegions.removeIf(rawRegion -> rawRegion.isReattempt() && loadRegion(rawRegion));
        log.info("Loaded " + (count - failedRegions.size()) + " of failed regions.");
    }

    @Nullable
    private RawRegion loadRaw(@NotNull ConfigurationSection section) {
        String name = section.getName();

        String minString = section.getString("Min");
        String maxString = section.getString("Max");

        boolean all = section.getBoolean("All", true);
        List<String> presets = section.getStringList("Presets");
        int priority = section.getInt("Priority", 1);

        RawRegion rawRegion = new RawRegion(name, minString, maxString, presets, all, priority);

        if (Strings.isNullOrEmpty(minString) || Strings.isNullOrEmpty(maxString)) {
            this.failedRegions.add(rawRegion);
            log.severe("Could not load region " + name + ", invalid location strings.");
            return null;
        }

        if (!Locations.isLocationLoaded(minString) || !Locations.isLocationLoaded(maxString)) {
            rawRegion.setReattempt(true);
            this.failedRegions.add(rawRegion);
            log.info("World for region " + name + " is not loaded. Reattempting after complete server load.");
            return null;
        }

        return rawRegion;
    }

    private void loadWorldRegion(ConfigurationSection section, String name) {
        String worldName = section.getString("worldName");

        RegenerationWorld world = new RegenerationWorld(name, worldName);
        world.setPriority(section.getInt("Priority", 1));
        world.setAll(section.getBoolean("All", true));

        List<String> presets = section.getStringList("Presets");

        for (String presetName : presets) {
            BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

            if (preset == null) {
                log.warning(String.format("Preset %s isn't loaded, but is included in region %s.", presetName, world.getName()));
            }

            world.addPreset(presetName);
        }

        log.fine(() -> String.format("Loaded regeneration world %s", world));
        this.loadedAreas.add(world);
    }

    public void load() {
        this.loadedAreas.clear();
        plugin.getFiles().getRegions().load();

        FileConfiguration regions = plugin.getFiles().getRegions().getFileConfiguration();

        ConfigurationSection parentSection = regions.getConfigurationSection("Regions");

        if (parentSection != null) {
            for (String name : parentSection.getKeys(false)) {
                ConfigurationSection section = parentSection.getConfigurationSection(name);

                // Shouldn't happen
                if (section == null) {
                    continue;
                }

                // Load a world region
                if (section.isSet("worldName")) {
                    loadWorldRegion(section, name);
                    continue;
                }

                // For normal regions we use RawRegion first to ensure it gets loaded (and later saved...).

                RawRegion rawRegion = loadRaw(section);
                if (rawRegion == null) {
                    continue;
                }
                loadRegion(rawRegion);
            }
        }

        this.sort();
        log.info("Loaded " + this.loadedAreas.size() + " region(s)...");
    }

    private boolean loadRegion(RawRegion rawRegion) {
        RegenerationRegion region = rawRegion.build();

        if (region == null) {
            log.warning("Could not load region " + rawRegion.getName() + ", world " + rawRegion.getMax() + " still not loaded.");
            return false;
        }

        // Attach presets
        for (String presetName : rawRegion.getBlockPresets()) {
            BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

            if (preset == null) {
                log.warning(String.format("Preset %s isn't loaded, but is included in region %s.", presetName, rawRegion.getName()));
            }

            region.addPreset(presetName);
        }

        this.loadedAreas.add(region);
        this.sort();
        log.fine(() -> "Loaded region " + region);
        return true;
    }

    // Only attempt to reload the presets configured as they could've changed.
    // Reloading whole regions could lead to the regeneration disabling. Could hurt the builds etc.
    // -- Changed to preset names for regions, no need to reload, just print a warning when a preset is not loaded.
    public void reload() {

        for (RegenerationArea area : this.loadedAreas) {
            Collection<String> presets = area.getPresets();

            // Attach presets
            for (String presetName : presets) {
                BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

                if (preset == null) {
                    log.warning(String.format("Preset %s isn't loaded, but is included in area %s.", presetName, area.getName()));
                }
            }
        }

        this.sort();
        log.info("Reloaded " + this.loadedAreas.size() + " region(s)...");
    }

    public void save() {
        FileConfiguration regions = plugin.getFiles().getRegions().getFileConfiguration();

        regions.set("Regions", null);

        ConfigurationSection root = ensureRegionsSection(regions);

        // Save failed regions to preserve them for next run. Maybe the world comes back.
        for (RawRegion rawRegion : new HashSet<>(this.failedRegions)) {
            ConfigurationSection regionSection = root.createSection(rawRegion.getName());

            regionSection.set("Min", rawRegion.getMin());
            regionSection.set("Max", rawRegion.getMax());

            regionSection.set("All", rawRegion.isAll());
            regionSection.set("Presets", rawRegion.getBlockPresets());
        }

        for (RegenerationArea area : new HashSet<>(this.loadedAreas)) {
            ConfigurationSection section = root.createSection(area.getName());
            area.serialize(section);
        }

        plugin.getFiles().getRegions().save();

        log.fine(() -> "Saved " + (this.loadedAreas.size() + this.failedRegions.size()) + " area(s)...");
    }

    private ConfigurationSection ensureRegionsSection(FileConfiguration configuration) {
        return configuration.contains("Regions") ? configuration.getConfigurationSection("Regions") : configuration.createSection("Regions");
    }

    public boolean exists(String name) {
        return this.loadedAreas.stream().anyMatch(r -> r.getName().equals(name));
    }

    public RegenerationArea getArea(@NotNull String name) {
        return this.loadedAreas.stream().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    public void removeArea(@NotNull String name) {
        Iterator<RegenerationArea> it = loadedAreas.iterator();
        while (it.hasNext()) {
            RegenerationArea area = it.next();

            if (Objects.equals(area.getName(), name)) {
                it.remove();
                break;
            }
        }
        this.sort();
    }

    @Nullable
    public RegenerationArea getArea(@NotNull Block block) {
        for (RegenerationArea area : this.loadedAreas) {
            if (area.contains(block)) {
                return area;
            }
        }
        return null;
    }

    public void addArea(@NotNull RegenerationArea region) {
        this.loadedAreas.add(region);
        this.sort();
        log.fine(() -> "Added area " + region);
        save();
    }

    @NotNull
    public List<RegenerationArea> getLoadedAreas() {
        return Collections.unmodifiableList(loadedAreas);
    }
}