package nl.aurorion.blockregen.region;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.util.BlockPosition;
import nl.aurorion.blockregen.util.Locations;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// todo: shouldn't be needed anymore
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

    @Nullable
    public CuboidRegion build() {
        Location min = Locations.locationFromString(this.min);
        Location max = Locations.locationFromString(this.max);

        if (min == null || max == null) {
            return null;
        }

        if (min.getWorld() == null || max.getWorld() == null) {
            return null;
        }

        CuboidRegion region = CuboidRegion.create(name, BlockPosition.from(min.getBlock()), BlockPosition.from(max.getBlock()));
        region.setDisableOtherBreak(disableOtherBreak);
        region.setPriority(priority);
        region.setAll(all);
        return region;
    }
}
