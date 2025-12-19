package nl.aurorion.blockregen.storage;

import nl.aurorion.blockregen.storage.exception.StorageException;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Create a StorageDriver from supplied configuration.
 */
public interface DriverProvider {

    /**
     * @throws StorageException When options required in order for the driver to work properly are missing or invalid.
     */
    @NotNull
    StorageDriver create(@Nullable ConfigurationSection section) throws StorageException;
}
