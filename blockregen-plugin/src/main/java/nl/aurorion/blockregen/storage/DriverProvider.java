package nl.aurorion.blockregen.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DriverProvider {
    // Create a storage driver with its configuration section.
    // It could be null if there's no config. Assume defaults or fail.
    // todo: custom exception when no config / bad values
    @NotNull
    StorageDriver create(@Nullable ConfigurationSection section);
}
