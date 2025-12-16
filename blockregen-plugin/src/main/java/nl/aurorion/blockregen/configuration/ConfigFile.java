package nl.aurorion.blockregen.configuration;

import com.google.common.base.Charsets;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class ConfigFile {

    private static final Pattern TARGET_MATERIAL_PATTERN = Pattern.compile("^\\s*target-material:\\s*(.+?)(\\s*#.*)?$", Pattern.CASE_INSENSITIVE);

    private final BlockRegenPlugin plugin;

    @Getter
    private final String path;

    @Getter
    private File file;

    @Getter
    private FileConfiguration fileConfiguration;

    @Getter
    @Setter
    private boolean forceEscapeTargetMaterial = false;

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

        // Force escape target-material when a glob pattern at the front is used avoiding YAML aliases.
        // target-material: *_ORE -> target-material: "*_ORE"

        try {
            BufferedReader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8);
            StringBuilder builder = new StringBuilder();

            String line;
            try {
                // The same as FileConfiguration#load with escapes added.
                while ((line = reader.readLine()) != null) {
                    if (!this.forceEscapeTargetMaterial) {
                        builder.append(line);
                        builder.append('\n');
                        continue;
                    }

                    Matcher matcher = TARGET_MATERIAL_PATTERN.matcher(line);

                    if (!matcher.matches()) {
                        builder.append(line);
                        builder.append('\n');
                        continue;
                    }

                    String value = matcher.group(1).trim();

                    if (value.startsWith("*") && !value.startsWith("\"") && !value.startsWith("'")) {
                        String escapedValue = "\"" + value + "\"";
                        String escapedLine = line.replaceFirst(Pattern.quote(value), escapedValue);
                        builder.append(escapedLine);
                    } else {
                        builder.append(line);
                    }
                    builder.append('\n');
                }
            } finally {
                reader.close();
            }

            YamlConfiguration config = new YamlConfiguration();
            try {
                config.loadFromString(builder.toString());
                this.fileConfiguration = config;
            } catch (InvalidConfigurationException e) {
                log.log(Level.SEVERE, "Invalid YAML configuration in " + this.path + ": " + e.getMessage(), e);
                this.fileConfiguration = new YamlConfiguration();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not read file " + this.path + ": " + e.getMessage(), e);
            this.fileConfiguration = new YamlConfiguration();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error processing file " + this.path + ": " + e.getMessage(), e);
            this.fileConfiguration = new YamlConfiguration();
        }

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