package nl.aurorion.blockregen.util;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A collection of parsing methods.
 * <p>
 * If a default value is provided as an argument, return that on failure. Otherwise, throw a {@link ParseException}.
 */
@Log
public class Parsing {

    /**
     * Attempt to run the {@param callable} containing the parse action. If it's unsuccessful, throw ParseException with
     * {@param message}.
     *
     * @throws ParseException If the parsing fails (an exception is thrown in the callable).
     */
    public static <T> T parse(@NotNull Supplier<T> callable, @NotNull String message) {
        try {
            return callable.get();
        } catch (Exception e) {
            throw new ParseException(message);
        }
    }

    @NotNull
    public static String notNull(@Nullable String input) {
        if (input == null) {
            throw new ParseException("Parsing input cannot be null.");
        }
        return input;
    }

    /**
     * Attempt to parse an int from {@param input}. If unsuccessful throw {@link ParseException}.
     *
     * @throws ParseException If the parsing fails.
     */
    public static int parseInt(@Nullable String input) {
        return parse(() -> Integer.parseInt(notNull(input)), "Invalid integer.");
    }

    /**
     * Attempt to parse an int from {@param input}. If unsuccessful throw {@link ParseException}.
     *
     * @throws ParseException If the parsing fails.
     */
    public static int parseInt(@Nullable String input, @NotNull String message) {
        return parse(() -> Integer.parseInt(notNull(input)), message);
    }

    /**
     * Attempt to parse an int from {@param input}. If unsuccessful return {@param def}.
     */
    public static int parseInt(@Nullable String input, int def) {
        try {
            return Integer.parseInt(notNull(input).trim());
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    /**
     * Attempt to parse a double from {@param input}.
     *
     * @throws ParseException with {@param message} if the parsing fails.
     */
    public static double parseDouble(@Nullable String input, @NotNull String message) {
        return parse(() -> Double.parseDouble(notNull(input)), message);
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public static XEnchantment parseEnchantment(@NotNull String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Enchantment input cannot be empty.");
        }

        XEnchantment xEnchantment = XEnchantment.of(input.trim())
                .orElseThrow(() -> new ParseException("Could not parse enchantment from '" + input + "'."));

        if (xEnchantment.get() == null) {
            throw new ParseException("Could not load enchantment from '" + input + "'.");
        }
        return xEnchantment;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    @Contract("null,_->fail")
    public static XMaterial parseMaterial(@Nullable String input, boolean blocksOnly) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Material input cannot be empty.");
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(input)
                .orElseThrow(() -> new ParseException("Could not parse material from '" + input + "'."));

        if (xMaterial.get() == null) {
            throw new ParseException("Could not load material from '" + input + "'.");
        }

        if (blocksOnly && !xMaterial.get().isBlock()) {
            throw new ParseException("Material '" + xMaterial + "' is not a block.");
        }
        return xMaterial;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    @Contract("null->fail")
    public static XMaterial parseMaterial(@Nullable String input) {
        return parseMaterial(input, false);
    }

    public static <E extends Enum<E>> E parseEnum(String str, Class<E> clazz) {
        return parseEnum(str, clazz, null);
    }

    public static <E extends Enum<E>> E parseEnum(String str, Class<E> clazz, Consumer<Throwable> exceptionCallback) {
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
}
