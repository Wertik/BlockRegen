package nl.aurorion.blockregen.storage.sqlite;

import nl.aurorion.blockregen.region.CuboidRegion;
import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import nl.aurorion.blockregen.storage.exception.InvalidDataException;
import nl.aurorion.blockregen.storage.exception.StorageException;
import nl.aurorion.blockregen.util.BlockPosition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Regions {

    private Regions() {
    }

    @Contract("null->null;!null->_")
    public static Boolean objToOptionalBoolean(@Nullable Object o) throws InvalidDataException {
        if (o == null) {
            return null;
        }

        if (o instanceof Integer) {
            return (int) o == 1;
        }

        throw new InvalidDataException("Invalid type for optional boolean " + o.getClass().getSimpleName());
    }

    @Contract("null->null;!null->_")
    public static Integer optionalBooleanToInt(Boolean bool) {
        return bool == null ? null : (bool ? 1 : 0);
    }

    @NotNull
    public static Region fromResultSet(@NotNull ResultSet resultSet) throws SQLException, InvalidDataException {
        String name = resultSet.getString("name");
        int priority = resultSet.getInt("priority");
        boolean all = resultSet.getBoolean("all");
        Boolean disableOtherBreak = objToOptionalBoolean(resultSet.getObject("disable_other_break"));

        String worldName = resultSet.getString("world_name");

        RegionType type = RegionType.values()[resultSet.getInt("type")];

        Region region;
        switch (type) {
            case CUBOID:
                String topLeft = resultSet.getString("cuboid_top_left");
                String bottomRight = resultSet.getString("cuboid_bottom_right");

                BlockPosition pos1 = BlockPosition.from(worldName, topLeft);
                BlockPosition pos2 = BlockPosition.from(worldName, bottomRight);

                region = CuboidRegion.create(name, pos1, pos2);
                break;
            case WORLD:
                region = WorldRegion.create(name, worldName);
                break;
            default:
                throw new InvalidDataException("Unsupported region type " + type.name());
        }

        region.setPriority(priority);
        region.setAll(all);
        region.setDisableOtherBreak(disableOtherBreak);

        return region;
    }

    public static RegionType getRegionType(@NotNull Region region) {
        if (region instanceof CuboidRegion) {
            return RegionType.CUBOID;
        } else if (region instanceof WorldRegion) {
            return RegionType.WORLD;
        } else {
            throw new IllegalArgumentException("Region type " + region.getClass().getSimpleName() + " not supported by SQLiteStorageDriver.");
        }
    }
}
