package nl.aurorion.blockregen;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.configuration.ConfigFile;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Log
public class LanguageManager {

    private final BlockRegen plugin;
    private final Map<String, String> loadedMessages = new HashMap<>();

    private FileConfiguration internalConfig;
    private ConfigFile config;

    private boolean insertPrefix = false;

    public LanguageManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public MessageWrapper get(String key) {
        String value = this.loadedMessages.get(key);
        return new MessageWrapper(plugin, insertPrefix ? "%prefix%" + value : value);
    }

    public String raw(String key) {
        return this.loadedMessages.get(key);
    }

    public void send(@NotNull CommandSender sender, @NotNull String key) {
        String value = this.loadedMessages.get(key);
        sender.sendMessage(insertPrefix ? "%prefix%" + value : value);
    }

    public void load() {
        // Load default Messages.yml, read the content.
        InputStream stream = plugin.getResource("Messages.yml");

        if (stream == null) {
            log.severe("Failed to load Messages.yml from the jar file. No new messages will be loaded.");
        } else {
            this.internalConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }

        // Load the one outside.
        this.config = new ConfigFile(plugin, "Messages.yml");

        if (!config.getFileConfiguration().contains("Insert-Prefix")) {
            config.getFileConfiguration().set("Insert-Prefix", true);
        }
        insertPrefix = config.getFileConfiguration().getBoolean("Insert-Prefix", true);

        // Load messages from the original, replace those contained in the configured one.

        boolean save = false;

        for (String key : Message.keys()) {
            String str = this.config.getFileConfiguration().getString("Messages." + key);
            if (str == null) {
                if (stream != null) {
                    String original = internalConfig.getString(key);
                    this.loadedMessages.put(key, original);
                    this.config.getFileConfiguration().set("Messages." + key, original);
                }

                save = true;
                continue;
            }
            this.loadedMessages.put(key, str);
        }

        if (save) {
            this.config.save();
        }
    }
}
