package nl.aurorion.blockregen.region;

import lombok.Getter;
import nl.aurorion.blockregen.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

public class WorldRegion extends RegionBase {

    @Getter
    private final String worldName;

    private WorldRegion(String name, String worldName) {
        super(name);
        this.worldName = worldName;
    }

    @NotNull
    public static WorldRegion create(@NotNull String name, @NotNull String worldName) {
        return new WorldRegion(name, worldName);
    }

    @Override
    public boolean contains(@NotNull BlockPosition position) {
        return position.getWorldName().equals(this.worldName);
    }

    @Override
    public String toString() {
        return "RegenerationWorld{" +
                "name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", presets=" + presets +
                ", all=" + all +
                ", priority=" + priority +
                '}';
    }
}
