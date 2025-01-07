package nl.aurorion.blockregen.configuration;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;

@Log
public class ConfigFile {

    @Getter
    private final String path;

    @Getter
    private FileConfiguration fileConfiguration;

    @Getter
    private File file;

    private final BlockRegenPlugin plugin;

    public ConfigFile(BlockRegenPlugin plugin, String path) {
        this.path = path.contains(".yml") ? path : path + ".yml";
        this.plugin = plugin;
    }

    public void load() {
        this.file = new File(plugin.getDataFolder(), this.path);

        if (!file.exists()) {
            try {
                plugin.saveResource(this.path, false);
            } catch (IllegalArgumentException e) {
                try {
                    if (!file.createNewFile())
                        log.severe("Could not create file " + this.path);
                } catch (IOException e1) {
                    log.severe("Could not create file " + this.path);
                    return;
                }
            }

            log.info("Created file " + this.path);
        }

        this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
        log.info("Loaded file " + this.path);
    }

    public void save() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            log.severe("Could not save " + this.path);
        }
    }
}