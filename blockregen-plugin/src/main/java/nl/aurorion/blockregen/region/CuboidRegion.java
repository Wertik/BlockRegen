package nl.aurorion.blockregen.region;

import lombok.Getter;
import nl.aurorion.blockregen.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CuboidRegion extends RegionBase {

    @Getter
    private final BlockPosition min;
    @Getter
    private final BlockPosition max;

    private CuboidRegion(@NotNull String name, @NotNull BlockPosition min, @NotNull BlockPosition max) {
        super(name);

        this.min = min;
        this.max = max;
    }

    /**
     * Create a new cuboid region. Ensures that the region has correctly set boundaries.
     *
     * @throws IllegalArgumentException If the positions are not in the same world.
     */
    @NotNull
    public static CuboidRegion create(@NotNull String name, @NotNull BlockPosition pos1, @NotNull BlockPosition pos2) throws IllegalArgumentException {
        if (!Objects.equals(pos1.getWorldName(), pos2.getWorldName())) {
            throw new IllegalArgumentException("CuboidRegion positions are not in the same world.");
        }

        BlockPosition min = BlockPosition.from(
                pos1.getWorldName(),
                Integer.min(pos1.getX(), pos2.getX()),
                Integer.min(pos1.getY(), pos2.getY()),
                Integer.min(pos1.getZ(), pos2.getZ())
        );

        BlockPosition max = BlockPosition.from(
                pos1.getWorldName(),
                Integer.max(pos1.getX(), pos2.getX()),
                Integer.max(pos1.getY(), pos2.getY()),
                Integer.max(pos1.getZ(), pos2.getZ())
        );

        return new CuboidRegion(name, min, max);
    }

    @Override
    public boolean contains(@NotNull BlockPosition position) {
        if (!max.getWorldName().equals(position.getWorldName())) {
            return false;
        }

        return position.getX() <= max.getX() && position.getX() >= min.getX()
                && position.getZ() <= max.getZ() && position.getZ() >= min.getZ()
                && position.getY() <= max.getY() && position.getY() >= min.getY();
    }

    @Override
    public String toString() {
        return "RegenerationRegion{" +
                "min=" + min +
                ", max=" + max +
                ", name='" + name + '\'' +
                ", presets=" + presets +
                ", all=" + all +
                ", priority=" + priority +
                '}';
    }
}