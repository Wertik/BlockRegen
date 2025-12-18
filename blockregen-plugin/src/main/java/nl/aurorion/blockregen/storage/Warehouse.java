package nl.aurorion.blockregen.storage;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.storage.exception.StorageException;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Log
public class Warehouse {

    private final BlockRegenPlugin plugin;

    private final Map<String, DriverProvider> options = new HashMap<>();

    @Getter
    private StorageDriver selectedDriver;

    public Warehouse(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerStorageProvider(@NotNull String key, DriverProvider storageDriver) {
        options.put(key, storageDriver);
        log.fine(() -> "Registered storage driver " + key);
    }

    public void initializeStorage() {
        ConfigurationSection storageSection = plugin.getFiles().getSettings().getFileConfiguration().getConfigurationSection("Storage");

        String driverKey = "sqlite";
        ConfigurationSection driverSection = null;
        if (storageSection == null) {
            log.warning(() -> "Storage section is not present, assuming SQLite.");
        } else {
            driverKey = storageSection.getString("Driver");

            if (driverKey == null) {
                log.warning(() -> "Storage driver not set. Assuming SQLite.");
                driverKey = "sqlite";
            }

            driverSection = storageSection.getConfigurationSection(driverKey);
        }

        DriverProvider provider = options.get(driverKey);

        this.selectedDriver = provider.create(driverSection);
        log.fine("Selected driver " + driverKey);

        log.fine(() -> "Initializing...");

        try {
            this.selectedDriver.initialize();
        } catch (StorageException exception) {
            // todo: stop the server? this could be hazardous
            log.severe(() -> "Failed to initialize storage: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
