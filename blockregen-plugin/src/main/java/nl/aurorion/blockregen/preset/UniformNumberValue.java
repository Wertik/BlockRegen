package nl.aurorion.blockregen.preset;

import lombok.Getter;
import nl.aurorion.blockregen.BlockRegenPluginImpl;

// A number value specified by a low and high bound.
// Randomly generated Uniform(low; high).
public class UniformNumberValue implements NumberValue {

    @Getter
    private final double low;
    @Getter
    private final double high;

    UniformNumberValue(double low, double high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public double getDouble() {
        return Math.max(BlockRegenPluginImpl.getInstance().getRandom().nextDouble() * high, low);
    }

    @Override
    public int getInt() {
        return (int) Math.round(getDouble());
    }

    @Override
    public String toString() {
        return "RangeNumberValue{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }
}
