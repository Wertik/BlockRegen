package nl.aurorion.blockregen.region;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
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

@Log
public class RegionManager {

    private final BlockRegenPlugin plugin;

    private List<Region> loadedRegions = new ArrayList<>();

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

    // Load all the regions.
    // If any of them fail to load.
    // We provide protection in the regions, if they're not loaded, buildings could be griefed.
    public void loadAll() throws StorageException {
        StorageDriver driver = plugin.getWarehouse().getSelectedDriver();
        this.loadedRegions = driver.loadRegions();
        log.info("Loaded " + loadedRegions.size() + " region(s)...");
    }

    public void save() throws StorageException {
        plugin.getWarehouse().getSelectedDriver().updateRegions(loadedRegions);
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