package nl.aurorion.blockregen.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class BukkitVersions {
    @NotNull
    public final static String CURRENT_VERSION = getVersion();

    private static String getVersion() {
        // ex.: 1.20.1-R0.1-SNAPSHOT
        String version = Bukkit.getServer().getBukkitVersion();

        // remove snapshot part
        version = version.substring(0, version.indexOf("-"));

        // remove patch version
        int lastDot = version.lastIndexOf(".");
        if (lastDot > 2) {
            version = version.substring(0, lastDot);
        }

        return version;
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
