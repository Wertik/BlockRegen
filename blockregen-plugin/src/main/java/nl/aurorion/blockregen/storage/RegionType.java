package nl.aurorion.blockregen.storage;

import nl.aurorion.blockregen.region.CuboidRegion;
import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import org.jetbrains.annotations.NotNull;

public enum RegionType {
    CUBOID,
    WORLD;

    @NotNull
    public static RegionType of(@NotNull Region region) throws IllegalArgumentException {
        if (region instanceof CuboidRegion) {
            return RegionType.CUBOID;
        } else if (region instanceof WorldRegion) {
            return RegionType.WORLD;
        } else {
            throw new IllegalArgumentException("Region type " + region.getClass().getSimpleName() + " not supported by SQLiteStorageDriver.");
        }
    }

    @NotNull
    public static RegionType of(int x) throws IllegalArgumentException {
        try {
            return RegionType.values()[x];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid region type index " + x + ".");
        }
    }
}
