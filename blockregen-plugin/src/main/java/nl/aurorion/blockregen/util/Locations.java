package nl.aurorion.blockregen.util;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class Locations {

    // Serialize location to string in an easily parsable way
    public String locationToString(Location loc) {
        World world = loc.getWorld();
        return world == null ? "" : world.getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    // Deserialize location from string.
    @Nullable
    public Location locationFromString(@Nullable String str) {
        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        String[] arr = str.split(";");
        World world = Bukkit.getWorld(arr[0]);

        if (world == null || arr.length < 4) {
            return null;
        }

        return new Location(world, Double.parseDouble(arr[1]), Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
    }

    // Check if the locations world is loaded.
    public boolean isLocationLoaded(@NotNull String str) {
        String[] arr = str.split(";");
        return Bukkit.getWorld(arr[0]) != null;
    }
}
