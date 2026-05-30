package nl.aurorion.blockregen.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class BukkitVersions {
    @NotNull
    public final static String CURRENT_VERSION = getVersion();

    private static String getVersion() {
        return Versions.extractMajorMinorVersion(Bukkit.getServer().getBukkitVersion());
    }

    public static boolean isCurrentAbove(@NotNull String versionString, boolean include) {
        int res = Versions.compareVersions(CURRENT_VERSION, versionString, -1);
        return include ? res >= 0 : res > 0;
    }

    public static boolean isCurrentBelow(@NotNull String versionString, boolean include) {
        int res = Versions.compareVersions(CURRENT_VERSION, versionString, -1);
        return include ? res <= 0 : res < 0;
    }
}
