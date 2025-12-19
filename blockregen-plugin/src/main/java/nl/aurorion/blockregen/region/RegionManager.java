package nl.aurorion.blockregen.region;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.region.selection.RegionSelection;
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

    public boolean finishSelection(@NotNull String regionName, @NotNull RegionSelection selection) throws StorageException {
        Location first = selection.getFirst();
        Location second = selection.getSecond();

        Region region = CuboidRegion.create(regionName, BlockPosition.from(first.getBlock()), BlockPosition.from(second.getBlock()));
        addRegion(region);
        return true;
    }

    // Load all the regions.
    // If any of them fail to load.
    // We provide protection in the regions, if they're not loaded, buildings could be griefed.
    public void loadAll() throws StorageException {
        this.loadedRegions = plugin.getWarehouse().ensureSelectedDriver().loadRegions();
        log.info("Loaded " + loadedRegions.size() + " region(s)...");
    }

    public void saveAll() throws StorageException {
        if (!plugin.getWarehouse().isInitialized()) {
            return;
        }

        plugin.getWarehouse().ensureSelectedDriver().updateRegions(loadedRegions);
    }

    public boolean exists(String name) {
        return this.loadedRegions.stream().anyMatch(r -> r.getName().equals(name));
    }

    public Region getRegion(@NotNull String name) {
        return this.loadedRegions.stream().filter(r -> r.getName().equals(name)).findAny().orElse(null);
    }

    public void deleteRegion(@NotNull String name) throws StorageException {
        if (!plugin.getWarehouse().isInitialized()) {
            throw new StorageException("Storage not initialized properly.");
        }

        Iterator<Region> it = this.loadedRegions.iterator();
        while (it.hasNext()) {
            Region region = it.next();

            if (Objects.equals(region.getName(), name)) {
                this.plugin.getWarehouse().ensureSelectedDriver().deleteRegion(region);
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

    public void addRegion(@NotNull Region region) throws StorageException {
        this.loadedRegions.add(region);

        this.plugin.getWarehouse().ensureSelectedDriver().saveRegion(region);

        this.sort();
        log.fine(() -> "Added region " + region);
    }

    @NotNull
    public List<Region> getLoadedRegions() {
        return Collections.unmodifiableList(this.loadedRegions);
    }
}