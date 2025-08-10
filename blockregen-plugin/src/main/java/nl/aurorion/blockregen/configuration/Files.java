package nl.aurorion.blockregen.configuration;

import lombok.Getter;
import nl.aurorion.blockregen.BlockRegenPlugin;

public class Files {

    @Getter
    private final ConfigFile settings;
    @Getter
    private final ConfigFile messages;
    @Getter
    private final ConfigFile blockList;
    @Getter
    private final ConfigFile regions;

    public Files(BlockRegenPlugin plugin) {
        this.settings = new ConfigFile(plugin, "Settings.yml");
        this.messages = new ConfigFile(plugin, "Messages.yml");
        this.blockList = new ConfigFile(plugin, "Blocklist.yml");
        this.regions = new ConfigFile(plugin, "Regions.yml");
    }

    public void load() {
        this.settings.load();
        this.messages.load();
        this.blockList.load();
        this.regions.load();
    }
}