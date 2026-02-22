package nl.aurorion.blockregen.preset.condition.expression;

import com.google.common.base.Strings;
import nl.aurorion.blockregen.Context;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Operand {
    Object value(Context ctx);

    Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%\\S+%)");

    @Log
    class Parser {
        @NotNull
        public static Object parseObject(String input) {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ignored) {
                // Not an integer
            }

            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException ignored) {
                // Not a double
            }
            try {
                return LocalTime.parse(input, DateTimeFormatter.ofPattern("H:m:s"));
            } catch (DateTimeParseException ignored) {
                // Not a date
            }

            return input;
        }

        /**
         * Parse either a constant or a placeholder variable.
         *
         * @throws ParseException If the parsing fails.
         */
        @NotNull
        static Operand parse(@NotNull String input) {
            if (Strings.isNullOrEmpty(input)) {
                throw new ParseException("Input cannot be null or empty");
            }

            // Variable or Constant
            // Based on whether it contains a placeholder format on one side.

            String trimmed = input.trim();

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(trimmed);

            if (matcher.find()) {
                return new Variable(matcher.group(1));
            } else {
                Object v = parseObject(trimmed);
                return new Constant(v);
            }
        }

    }
}
