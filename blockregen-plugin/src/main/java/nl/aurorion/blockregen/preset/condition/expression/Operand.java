package nl.aurorion.blockregen.preset.condition.expression;

import com.google.common.base.Strings;
import com.linecorp.conditional.ConditionContext;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.util.Parsing;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Operand {
    Object value(ConditionContext ctx);

    Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%\\w+%)");

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

            try {
                return Parsing.parseEnchantment(input);
            } catch (ParseException ignored) {
                // Not an enchantment
            }

            return input;
        }

        /**
         * @throws IllegalArgumentException If the input is null or empty.
         */
        @NotNull
        static Operand parse(@NotNull String input) {
            if (Strings.isNullOrEmpty(input)) {
                throw new IllegalArgumentException("Input cannot be null or empty");
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
