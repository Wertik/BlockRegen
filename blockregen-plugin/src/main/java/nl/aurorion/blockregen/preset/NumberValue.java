package nl.aurorion.blockregen.preset;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.util.Parsing;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A number value parsed from configuration.
public interface NumberValue {
    double getDouble();

    int getInt();

    @NotNull
    static UniformNumberValue uniform(double low, double high) {
        return new UniformNumberValue(Math.min(low, high), Math.max(low, high));
    }

    @NotNull
    static FixedNumberValue fixed(double value) {
        return new FixedNumberValue(value);
    }

    @Log
    class Parser {
        private static final Pattern DASH_PATTERN = Pattern.compile("(-?\\d+)-(-?\\d+)");

        /**
         * Parse a number value from a string input.
         *
         * @throws ParseException If the parsing fails.
         */
        @NotNull
        @Contract("null->fail")
        public static NumberValue parse(String input) {
            if (Strings.isNullOrEmpty(input)) {
                throw new ParseException("No input supplied.");
            }

            Matcher matcher = DASH_PATTERN.matcher(input);
            log.finer(() -> "Input: " + input);

            if (matcher.matches()) {
                // If the matcher matches, it has to be a valid double.
                // But just in case, keep these errors.
                double low = Parsing.parseDouble(matcher.group(1), "Invalid value for low supplied: '" + matcher.group(1) + "'.");
                double high = Parsing.parseDouble(matcher.group(2), "Invalid value for high supplied: '" + matcher.group(1) + "'.");

                return NumberValue.uniform(low, high);
            }

            double value = Parsing.parseDouble(input, "Invalid value supplied: '" + input + "'.");
            return NumberValue.fixed(value);
        }

        /**
         * @param node The object to load from. String/Number/ConfigurationSection.
         * @return The parsed NumberValue.
         * @throws ParseException If the parsing fails.
         */
        @NotNull
        public static NumberValue load(@NotNull Object node) {
            Objects.requireNonNull(node);

            if (node instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection) node;

                if (!(section.get("low") instanceof Number) || !(section.get("high") instanceof Number)) {
                    throw new ParseException("Invalid properties for values.");
                }

                double low = section.getDouble("low");
                double high = section.getDouble("high");

                return NumberValue.uniform(low, high);
            } else {
                // Assume it's a String or a Number (either way we can parse it again)
                return parse(String.valueOf(node));
            }
        }
    }
}
