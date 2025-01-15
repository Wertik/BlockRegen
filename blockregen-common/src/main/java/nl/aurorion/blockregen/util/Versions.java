package nl.aurorion.blockregen.util;

import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;

public class Versions {

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
                num1 = ParseUtil.parseInt(arr1[i]);
            } catch (ParseException e) {
                throw new ParseException("Invalid version part '" + arr1[i] + "'");
            }

            int num2;
            try {
                num2 = ParseUtil.parseInt(arr2[i]);
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
