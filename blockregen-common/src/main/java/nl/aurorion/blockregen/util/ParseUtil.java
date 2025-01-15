package nl.aurorion.blockregen.util;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Log
@UtilityClass
public class ParseUtil {

    public double parseDouble(String input, Supplier<Double> onError) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return onError.get();
        }
    }

    /**
     * Attempt to parse an integer, return -1 if a NumberFormatException was thrown.
     */
    public int parseInt(String input, int def) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    public Integer parseInteger(@Nullable String input) {
        if (input == null) {
            return null;
        }

        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public int parseInt(String input) {
        return parseInt(input, -1);
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public XEnchantment parseEnchantment(@NotNull String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Enchantment input cannot be empty.");
        }

        XEnchantment xEnchantment = XEnchantment.of(input.trim()).orElseThrow(() -> new ParseException("Could not parse enchantment from '" + input + "'."));

        if (xEnchantment.get() == null) {
            throw new ParseException("Could not parse enchantment from '" + input + "'.");
        }
        return xEnchantment;
    }

    @Nullable
    public XMaterial parseMaterial(String input) {
        return parseMaterial(input, false);
    }

    @Nullable
    public XMaterial parseMaterial(String input, boolean blocksOnly) {

        if (Strings.isNullOrEmpty(input)) {
            return null;
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(input);

        if (!xMaterial.isPresent()) {
            log.fine(() -> "Could not parse material " + input);
            return null;
        }

        Material material = xMaterial.get().get();

        if (material != null && blocksOnly && !material.isBlock()) {
            log.fine(() -> "Material " + input + " is not a block.");
            return null;
        }

        return xMaterial.get();
    }

    public <E extends Enum<E>> E parseEnum(String str, Class<E> clazz) {
        return parseEnum(str, clazz, null);
    }

    public <E extends Enum<E>> E parseEnum(String str, Class<E> clazz, Consumer<Throwable> exceptionCallback) {

        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        try {
            return E.valueOf(clazz, str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            if (exceptionCallback != null)
                exceptionCallback.accept(e);
            return null;
        }
    }

    public <T> T nullOrDefault(Supplier<T> supplier, T def, Consumer<Throwable> exceptionCallback) {
        try {
            T t = supplier.get();
            return t == null ? def : t;
        } catch (Exception e) {
            exceptionCallback.accept(e);
            return def;
        }
    }

    public <T> T nullOrDefault(Supplier<T> supplier, T def) {
        try {
            T t = supplier.get();
            return t == null ? def : t;
        } catch (Exception e) {
            return def;
        }
    }

    public int compareVersions(String version1, String version2) {
        return compareVersions(version1, version2, -1);
    }

    // Compare simple semver
    public int compareVersions(String version1, String version2, int depth) {
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

            int num1 = ParseUtil.parseInteger(arr1[i]);
            int num2 = ParseUtil.parseInteger(arr2[i]);

            if (num1 > num2) {
                return 1;
            } else if (num2 > num1) {
                return -1;
            }
        }

        return 0;
    }
}
