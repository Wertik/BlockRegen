package nl.aurorion.blockregen.region.struct;

import lombok.Getter;
import nl.aurorion.blockregen.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class RegenerationRegion extends RegenerationArea {

    @Getter
    private final Location min;
    @Getter
    private final Location max;

    public RegenerationRegion(String name, Location min, Location max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean contains(@NotNull Block block) {
        // Check world
        if (max.getWorld() != null && !max.getWorld().equals(block.getWorld())) {
            return false;
        }

        // Check coordinates
        return block.getX() <= max.getX() && block.getX() >= min.getX()
                && block.getZ() <= max.getZ() && block.getZ() >= min.getZ()
                && block.getY() <= max.getY() && block.getY() >= min.getY();
    }

    @Override
    public void serialize(ConfigurationSection section) {
        super.serialize(section);
        section.set("Min", Locations.locationToString(this.min));
        section.set("Max", Locations.locationToString(this.max));
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