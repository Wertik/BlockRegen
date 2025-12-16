package nl.aurorion.blockregen.region;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.BlockPosition;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.region.selection.RegionSelection;
import nl.aurorion.blockregen.storage.StorageDriver;
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

    private final List<Region> loadedAreas = new ArrayList<>();

    // Set of regions that failed to load.
    private final Set<RawRegion> failedRegions = new HashSet<>();

    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public RegionManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public void sort() {
        loadedAreas.sort((o1, o2) -> Comparator.comparing(Region::getPriority).reversed().compare(o1, o2));
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
    public CuboidRegion createCuboidRegion(@NotNull String name, @NotNull RegionSelection selection) {
        Location first = selection.getFirst();
        Location second = selection.getSecond();

        return CuboidRegion.create(name, BlockPosition.from(first.getBlock()), BlockPosition.from(second.getBlock()));
    }

    public boolean finishSelection(@NotNull String name, @NotNull RegionSelection selection) {
        CuboidRegion region = createCuboidRegion(name, selection);
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

    public void load() {
        loadedAreas.clear();

        StorageDriver driver = plugin.getWarehouse().getSelectedDriver();

        Future<List<Region>> future = driver.loadRegions();

        try {
            loadedAreas.addAll(future.get(6000, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // todo
            throw new RuntimeException(e);
        }

        log.info("Loaded " + loadedAreas.size() + " region(s)...");
    }


    // Only attempt to reload the presets configured as they could've changed.
    // Reloading whole regions could lead to the regeneration disabling. Could hurt the builds etc.
    // -- Changed to preset names for regions, no need to reload, just print a warning when a preset is not loaded.
    public void reload() {

        for (Region area : this.loadedAreas) {
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
        Future<Void> future = plugin.getWarehouse().getSelectedDriver().updateRegions(loadedAreas);

        try {
            future.get(6000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // todo
            throw new RuntimeException(e);
        }
    }

    public boolean exists(String name) {
        return this.loadedAreas.stream().anyMatch(r -> r.getName().equals(name));
    }

    public Region getRegion(@NotNull String name) {
        return this.loadedAreas.stream().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    public void removeRegion(@NotNull String name) {
        Iterator<Region> it = this.loadedAreas.iterator();
        while (it.hasNext()) {
            Region region = it.next();

            if (Objects.equals(region.getName(), name)) {
                this.plugin.getWarehouse().getSelectedDriver().deleteRegion(region);
                it.remove();
                break;
            }
        }
        this.sort();
    }

    @Nullable
    public Region getRegion(@NotNull Block block) {
        BlockPosition position = BlockPosition.from(block);
        for (Region region : this.loadedAreas) {
            if (region.contains(position)) {
                return region;
            }
        }
        return null;
    }

    public void addRegion(@NotNull Region region) {
        this.loadedAreas.add(region);
        this.plugin.getWarehouse().getSelectedDriver().saveRegion(region);
        this.sort();
        log.fine(() -> "Added area " + region);
    }

    @NotNull
    public List<Region> getLoadedRegions() {
        return Collections.unmodifiableList(this.loadedAreas);
    }
}