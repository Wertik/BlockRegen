package nl.aurorion.blockregen.region.struct;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class RegenerationWorld extends RegenerationArea {

    @Getter
    private final String worldName;

    public RegenerationWorld(String name, String worldName) {
        super(name);
        this.worldName = worldName;
    }

    @Override
    public boolean contains(@NotNull Block block) {
        return block.getWorld().getName().equals(this.worldName);
    }

    @Override
    public void serialize(ConfigurationSection section) {
        super.serialize(section);
        section.set("worldName", this.worldName);
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
