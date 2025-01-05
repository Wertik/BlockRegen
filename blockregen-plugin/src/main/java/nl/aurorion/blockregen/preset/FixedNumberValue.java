package nl.aurorion.blockregen.preset;

import lombok.Getter;

// A number value parsed from the configuration. Holds a single number.
public class FixedNumberValue implements NumberValue {

    @Getter
    private final double value;

    FixedNumberValue(double value) {
        this.value = value;
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
