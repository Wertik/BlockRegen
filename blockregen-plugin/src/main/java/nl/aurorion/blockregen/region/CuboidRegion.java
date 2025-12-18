package nl.aurorion.blockregen.region;

import lombok.Getter;
import nl.aurorion.blockregen.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CuboidRegion extends RegionBase {

    @Getter
    private final BlockPosition bottomLeft;
    @Getter
    private final BlockPosition topRight;

    private CuboidRegion(@NotNull String name, @NotNull BlockPosition bottomLeft, @NotNull BlockPosition topRight) {
        super(name);
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    @NotNull
    public static CuboidRegion create(@NotNull String name, @NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        if (!Objects.equals(pos1.getWorldName(), pos2.getWorldName())) {
            throw new IllegalArgumentException("CuboidRegion positions are not in the same world.");
        }

        BlockPosition bottomLeft = BlockPosition.from(
                pos1.getWorldName(),
                Integer.min(pos1.getX(), pos2.getX()),
                Integer.min(pos1.getY(), pos2.getY()),
                Integer.min(pos1.getZ(), pos2.getZ())
        );

        BlockPosition topRight = BlockPosition.from(
                pos1.getWorldName(),
                Integer.max(pos1.getX(), pos2.getX()),
                Integer.max(pos1.getY(), pos2.getY()),
                Integer.max(pos1.getZ(), pos2.getZ())
        );

        return new CuboidRegion(name, bottomLeft, topRight);
    }

    @Override
    public boolean contains(@NotNull BlockPosition position) {
        if (!topRight.getWorldName().equals(position.getWorldName())) {
            return false;
        }

        return position.getX() <= topRight.getX() && position.getX() >= bottomLeft.getX()
                && position.getZ() <= topRight.getZ() && position.getZ() >= bottomLeft.getZ()
                && position.getY() <= topRight.getY() && position.getY() >= bottomLeft.getY();
    }

    @Override
    public String toString() {
        return "RegenerationRegion{" +
                "min=" + bottomLeft +
                ", max=" + topRight +
                ", name='" + name + '\'' +
                ", presets=" + presets +
                ", all=" + all +
                ", priority=" + priority +
                '}';
    }
}