package nl.aurorion.blockregen.storage;

import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.storage.exception.StorageException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface StorageDriver {
    void initialize() throws StorageException;

    // Returned regions are expected to be sorted by priority.
    @NotNull List<Region> loadRegions() throws StorageException;

    void saveRegion(@NotNull Region region) throws StorageException;

    void updateRegions(@NotNull Collection<Region> regions) throws StorageException;

    void deleteRegion(@NotNull Region region) throws StorageException;
}
