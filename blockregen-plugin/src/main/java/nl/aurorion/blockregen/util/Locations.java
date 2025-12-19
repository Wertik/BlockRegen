package nl.aurorion.blockregen.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Locations {

    /**
     * Format a location. Ignores pitch and yaw.
     *
     * @param location Location to format.
     */
    @NotNull
    public String format(@NotNull Location location) {
        World world = location.getWorld();
        return "Location{world=" + (world == null ? "null" : world.getName()) + "; x=" + location.getX() + "; y=" + location.getY() + "; z=" + location.getZ() + "}";
    }
}
