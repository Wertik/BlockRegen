package nl.aurorion.blockregen.region.struct;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.util.Locations;
import org.bukkit.Location;

import java.util.List;

public class RawRegion {

    @Getter
    private final String name;
    @Getter
    private final String min;
    @Getter
    private final String max;

    @Getter
    private final List<String> blockPresets;

    @Getter
    private final boolean all;

    @Getter
    private final int priority;

    @Getter
    private final Boolean disableOtherBreak;

    @Getter
    @Setter
    private boolean reattempt = false;

    public RawRegion(String name, String min, String max, List<String> blockPresets, boolean all, int priority, Boolean disableOtherBreak) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.blockPresets = blockPresets;
        this.all = all;
        this.priority = priority;
        this.disableOtherBreak = disableOtherBreak;
    }

    public RegenerationRegion build() {
        Location min = Locations.locationFromString(this.min);
        Location max = Locations.locationFromString(this.max);

        if (min == null || max == null) {
            return null;
        }

        Location actualMin = new Location(min.getWorld(), Double.min(min.getX(), max.getX()), Double.min(min.getY(), max.getY()), Double.min(min.getZ(), max.getZ()));
        Location actualMax = new Location(min.getWorld(), Double.max(min.getX(), max.getX()), Double.max(min.getY(), max.getY()), Double.max(min.getZ(), max.getZ()));

        RegenerationRegion region = new RegenerationRegion(name, actualMin, actualMax);
        region.setDisableOtherBreak(disableOtherBreak);
        region.setPriority(priority);
        region.setAll(all);
        return region;
    }
}
