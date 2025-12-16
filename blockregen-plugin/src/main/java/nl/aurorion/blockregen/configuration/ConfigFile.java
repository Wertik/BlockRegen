package nl.aurorion.blockregen.configuration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;

@Log
public class ConfigFile {

    private static final Pattern TARGET_MATERIAL_PATTERN = Pattern.compile("^\\s*target-material:\\s*(.+?)(\\s*#.*)?$", Pattern.CASE_INSENSITIVE);

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

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> processedLines = new ArrayList<>();
            
            for (String line : lines) {
                java.util.regex.Matcher matcher = TARGET_MATERIAL_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String value = matcher.group(1).trim();
                    
                    if (value.startsWith("*") && !value.startsWith("\"") && !value.startsWith("'")) {
                        String escapedValue = "\"" + value + "\"";
                        String escapedLine = line.replaceFirst(Pattern.quote(value), escapedValue);
                        processedLines.add(escapedLine);
                    } else {
                        processedLines.add(line);
                    }
                } else {
                    processedLines.add(line);
                }
            }
            
            String processedContent = String.join("\n", processedLines);
            YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(new StringReader(processedContent));
                this.fileConfiguration = config;
            } catch (org.bukkit.configuration.InvalidConfigurationException e) {
                log.severe("Invalid YAML configuration in " + this.path + ": " + e.getMessage());
                this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
            }
        } catch (IOException e) {
            log.severe("Could not read file " + this.path + ": " + e.getMessage());
            this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            log.severe("Error processing file " + this.path + ": " + e.getMessage());
            this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
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