package nl.aurorion.blockregen.util;

import nl.aurorion.blockregen.ParseException;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class Versions {

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
        int res = Versions.compareVersions(CURRENT_VERSION, versionString, 2);
        return include ? res >= 0 : res > 0;
    }

    public static boolean isCurrentBelow(@NotNull String versionString, boolean include) {
        int res = Versions.compareVersions(CURRENT_VERSION, versionString, 2);
        return include ? res <= 0 : res < 0;
    }

    public static int compareVersions(@NotNull String version1, @NotNull String version2) {
        return compareVersions(version1, version2, -1);
    }

    // Compare simple semver
    public static int compareVersions(@NotNull String version1, @NotNull String version2, int depth) {
        // Compare major
        String[] arr1 = version1.split("-")[0].split("\\.");
        String[] arr2 = version2.split("-")[0].split("\\.");

        int len = depth == -1 ? Math.max(arr1.length, arr2.length) : depth;

        for (int i = 0; i < len; i++) {

            if (arr1.length < i) {
                return -1;
            } else if (arr2.length < i) {
                return 1;
            }

            int num1;
            try {
                num1 = Parsing.parseInt(arr1[i]);
            } catch (ParseException e) {
                throw new ParseException("Invalid version part '" + arr1[i] + "'");
            }

            int num2;
            try {
                num2 = Parsing.parseInt(arr2[i]);
            } catch (ParseException e) {
                throw new ParseException("Invalid version part '" + arr2[i] + "'");
            }

            if (num1 > num2) {
                return 1;
            } else if (num2 > num1) {
                return -1;
            }
        }
        return 0;
    }
}
