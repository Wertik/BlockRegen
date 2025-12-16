package nl.aurorion.blockregen.storage;

import nl.aurorion.blockregen.region.Region;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public interface StorageDriver {
    void initialize() throws StorageException;

    // Returned regions are expected to be sorted by priority.
    @NotNull Future<List<Region>> loadRegions() throws StorageException;

    Future<Void> saveRegion(@NotNull Region region) throws StorageException;
    Future<Void> updateRegions(@NotNull Collection<Region> regions) throws StorageException;

    Future<Void> deleteRegion(@NotNull Region region) throws StorageException;
}
