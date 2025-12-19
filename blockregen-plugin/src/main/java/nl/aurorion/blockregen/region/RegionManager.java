package nl.aurorion.blockregen.region;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.LoadException;
import nl.aurorion.blockregen.configuration.ConfigFile;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.region.selection.RegionSelection;
import nl.aurorion.blockregen.storage.StorageDriver;
import nl.aurorion.blockregen.storage.exception.StorageException;
import nl.aurorion.blockregen.util.BlockPosition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
public class RegionManager {

    private final BlockRegenPlugin plugin;

    private List<Region> loadedRegions = new ArrayList<>();

    // Set of regions that failed to load.
    private final Set<RawRegion> failedRegions = new HashSet<>();

    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public RegionManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public void sort() {
        loadedRegions.sort((o1, o2) -> Comparator.comparing(Region::getPriority).reversed().compare(o1, o2));
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

    public boolean finishSelection(@NotNull String regionName, @NotNull RegionSelection selection) {
        Location first = selection.getFirst();
        Location second = selection.getSecond();

        Region region = CuboidRegion.create(regionName, BlockPosition.from(first.getBlock()), BlockPosition.from(second.getBlock()));
        addRegion(region);
        return true;
    }

    @NotNull
    public WorldRegion createWorldRegion(@NotNull String name, @NotNull String worldName) {
        return WorldRegion.create(name, worldName);
    }

    /*public void reattemptLoad() {
        if (failedRegions.isEmpty()) {
            return;
        }

        log.info("Reattempting to load regions...");
        int count = failedRegions.size();
        failedRegions.removeIf(rawRegion -> rawRegion.isReattempt() && loadRegion(rawRegion));
        log.info("Loaded " + (count - failedRegions.size()) + " of failed regions.");
    }*/

    // Load all the regions.
    // If any of them fail to load.
    // We provide protection in the regions, if they're not loaded, buildings could be grieved.
    public void loadAll() throws LoadException {
        StorageDriver driver = plugin.getWarehouse().getSelectedDriver();

        List<Region> regions;
        try {
            regions = driver.loadRegions();
        } catch (StorageException e) {
            // This state should be unrecoverable -- regions didn't load properly, we cannot guarantee block protection.
            throw new LoadException(e);
        }

        // Presets can be left unchecked?

        this.loadedRegions = regions;

        log.info("Loaded " + loadedRegions.size() + " region(s)...");
    }


    // Only attempt to reload the presets configured as they could've changed.
    // Reloading whole regions could lead to the regeneration disabling. Could hurt the builds etc.
    // -- Changed to preset names for regions, no need to reload, just print a warning when a preset is not loaded.
    public void reload() {

        for (Region area : this.loadedRegions) {
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
        log.info("Reloaded " + this.loadedRegions.size() + " region(s)...");
    }

    public void save() {
        try {
            plugin.getWarehouse().getSelectedDriver().updateRegions(loadedRegions);
        } catch (StorageException e) {
            // todo
        }
    }

    public boolean exists(String name) {
        return this.loadedRegions.stream().anyMatch(r -> r.getName().equals(name));
    }

    public Region getRegion(@NotNull String name) {
        return this.loadedRegions.stream().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    public void removeRegion(@NotNull String name) {
        Iterator<Region> it = this.loadedRegions.iterator();
        while (it.hasNext()) {
            Region region = it.next();

            if (Objects.equals(region.getName(), name)) {
                try {
                    this.plugin.getWarehouse().getSelectedDriver().deleteRegion(region);
                } catch (StorageException e) {
                    // todo
                    throw new RuntimeException(e);
                }
                it.remove();
                break;
            }
        }
        this.sort();
    }

    @Nullable
    public Region getRegion(@NotNull Block block) {
        BlockPosition position = BlockPosition.from(block);
        for (Region region : this.loadedRegions) {
            if (region.contains(position)) {
                return region;
            }
        }
        return null;
    }

    public void addRegion(@NotNull Region region) {
        this.loadedRegions.add(region);
        try {
            this.plugin.getWarehouse().getSelectedDriver().saveRegion(region);
        } catch (StorageException e) {
            // todo
            throw new RuntimeException(e);
        }
        this.sort();
        log.fine(() -> "Added area " + region);
    }

    @NotNull
    public List<Region> getLoadedRegions() {
        return Collections.unmodifiableList(this.loadedRegions);
    }
}