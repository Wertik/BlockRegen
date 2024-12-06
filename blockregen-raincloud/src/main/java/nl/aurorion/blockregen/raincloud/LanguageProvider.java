package nl.aurorion.blockregen.raincloud;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LanguageProvider {

    // Provide a translation based on its key.
    @Nullable
    String get(@NotNull CommandSender sender, @NotNull String key);
}
