package nl.aurorion.blockregen.region.struct;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// An area of regeneration.
// Either a whole world or a cuboid region (so far).
public abstract class RegenerationArea {
    @Getter
    protected final String name;

    protected final Set<String> presets = new HashSet<>();

    @Setter
    @Getter
    protected boolean all = true;

    // After changing the priority, always call RegionManager#sort to resort the regions.
    @Getter
    @Setter
    protected int priority = 1;

    public RegenerationArea(String name) {
        this.name = name;
    }

    public abstract boolean contains(@NotNull Block block);

    public void serialize(ConfigurationSection section) {
        section.set("All", this.all);
        section.set("Presets", new ArrayList<>(this.presets));
        section.set("Priority", this.priority);
    }

    public boolean switchAll() {
        setAll(!isAll());
        return isAll();
    }

    public boolean hasPreset(@Nullable String preset) {
        return all || (preset != null && this.presets.contains(preset));
    }

    public void addPreset(@NotNull String preset) {
        this.presets.add(preset);
    }

    public void removePreset(@NotNull String preset) {
        this.presets.remove(preset);
    }

    public void clearPresets() {
        this.presets.clear();
    }

    public @NotNull Collection<String> getPresets() {
        return Collections.unmodifiableCollection(this.presets);
    }

    @Override
    public String toString() {
        return "RegenerationArea{" +
                "name='" + name + '\'' +
                ", presets=" + presets +
                ", all=" + all +
                ", priority=" + priority +
                '}';
    }
}
