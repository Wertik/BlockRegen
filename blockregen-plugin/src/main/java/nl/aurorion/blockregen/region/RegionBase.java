package nl.aurorion.blockregen.region;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class RegionBase implements Region {

    @Getter
    protected final String name;

    protected final Set<String> presets = new HashSet<>();

    @Getter
    @Setter
    protected boolean all = true;

    @Getter
    @Setter
    @Nullable
    // null => take from Settings.yml
    protected Boolean disableOtherBreak = null;

    // After changing the priority, always call RegionManager#sort to resort the regions.
    @Getter
    @Setter
    protected int priority = 1;

    public RegionBase(String name) {
        this.name = name;
    }

    @Override
    public boolean hasPreset(@Nullable String preset) {
        return all || (preset != null && this.presets.contains(preset));
    }

    @Override
    public void addPreset(@NotNull String preset) {
        this.presets.add(preset);
    }

    @Override
    public void removePreset(@NotNull String preset) {
        this.presets.remove(preset);
    }

    @Override
    public void clearPresets() {
        this.presets.clear();
    }

    @Override
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
                ", disableOtherBreak=" + this.disableOtherBreak +
                '}';
    }
}
