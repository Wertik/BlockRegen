package nl.aurorion.blockregen.regeneration.struct;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

@Data
public class SimpleLocation {

    private String world;
    private int x, y, z;

    public SimpleLocation(Block block) {

        if (block == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        this.world = block.getWorld().getName();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleLocation that = (SimpleLocation) o;
        return that.x == x &&
                that.y == y &&
                that.z == z &&
                Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    public Block toBlock() {
        World world = Bukkit.getWorld(this.world);
        return world == null ? null : world.getBlockAt(this.x, this.y, this.z);
    }
}