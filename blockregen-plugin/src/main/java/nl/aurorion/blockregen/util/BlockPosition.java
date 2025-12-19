package nl.aurorion.blockregen.util;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.ParseException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BlockPosition {

    @Getter
    @Setter
    @NotNull
    private String worldName;

    @Getter
    @Setter
    private int x, y, z;

    // cache block access
    private transient Block block;

    private BlockPosition(@NotNull String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static @NotNull BlockPosition from(@NotNull Block block) {
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static @NotNull BlockPosition from(@NotNull String worldName, int x, int y, int z) {
        return new BlockPosition(worldName, x, y, z);
    }

    public static @NotNull BlockPosition from(@NotNull String worldName, @NotNull String serialized) throws ParseException {
        String[] parts = serialized.split(";");

        if (parts.length != 3) {
            throw new ParseException("Bad serialization format for a block position " + serialized + ".");
        }

        int x = Parsing.parseInt(parts[0]);
        int y = Parsing.parseInt(parts[1]);
        int z = Parsing.parseInt(parts[2]);

        return new BlockPosition(worldName, x, y, z);
    }

    @NotNull
    public String serialize() {
        return this.x + ";" + this.y + ";" + this.z;
    }

    @Nullable
    public Block toBlock() {
        if (this.block != null) {
            return this.block;
        }

        World world = Bukkit.getWorld(this.worldName);

        if (world == null) {
            return null;
        }

        this.block = world.getBlockAt(this.x, this.y, this.z);
        return this.block;
    }

    @Override
    public String toString() {
        return "BlockPosition{worldName=" + this.worldName + "; x=" + this.x + "; y=" + this.y + "; z=" + this.z + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPosition that = (BlockPosition) o;
        return that.x == x &&
                that.y == y &&
                that.z == z &&
                Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }
}