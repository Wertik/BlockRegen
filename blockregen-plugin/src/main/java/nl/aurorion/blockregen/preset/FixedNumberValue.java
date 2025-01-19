package nl.aurorion.blockregen.preset;

import com.google.common.base.Strings;
import lombok.Getter;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.util.Parsing;
import org.jetbrains.annotations.NotNull;

// A number value parsed from the configuration. Holds a single number.
public class FixedNumberValue implements NumberValue {

    @Getter
    private final double value;

    FixedNumberValue(double value) {
        this.value = value;
    }

    /**
     * @throws IllegalArgumentException If the supplied object {@param o} is of an unsupported type.
     */
    @NotNull
    public static FixedNumberValue from(@NotNull Object o) {
        if (o instanceof Number) {
            return FixedNumberValue.of((Number) o);
        } else if (o instanceof String) {
            return FixedNumberValue.parse((String) o);
        } else {
            throw new ParseException("Invalid type '" + o.getClass().getSimpleName() + "' for number value.");
        }
    }

    @NotNull
    public static FixedNumberValue of(@NotNull Number n) {
        return new FixedNumberValue(n.doubleValue());
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public static FixedNumberValue parse(String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("No input for FixedNumberValue.");
        }
        double value = Parsing.parseDouble(input, "Invalid value supplied: '" + input + "'.");
        return new FixedNumberValue(value);
    }

    @Override
    public double getDouble() {
        return value;
    }

    // Round instead of direct conversion. Seems more intuitive.
    @Override
    public int getInt() {
        return (int) Math.round(value);
    }

    @Override
    public String toString() {
        return "FixedNumberValue{" +
                "value=" + value +
                '}';
    }
}
