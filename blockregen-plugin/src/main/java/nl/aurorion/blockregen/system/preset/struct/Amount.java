package nl.aurorion.blockregen.system.preset.struct;

import nl.aurorion.blockregen.BlockRegen;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class Amount {

    private static final Pattern DASH_PATTERN = Pattern.compile("(-?\\\\d+)\\\\-(-?\\\\d+)");

    @Getter
    @Setter
    private double lowValue;
    @Getter
    @Setter
    private double highValue;

    @Getter
    @Setter
    private double fixedValue;

    @Getter
    @Setter
    private boolean fixed;

    public Amount(double low, double high) {
        fixed = false;

        lowValue = Math.min(low, high);
        highValue = Math.max(low, high);
    }

    public Amount(double fixedValue) {
        fixed = true;

        this.fixedValue = fixedValue;
    }

    // Load Amount from yaml
    public static Amount load(ConfigurationSection root, String path, double defaultValue) {

        if (root == null || root.get(path) == null) {
            return new Amount(defaultValue);
        }

        // low & high section syntax
        if (root.isConfigurationSection(path)) {
            ConfigurationSection section = root.getConfigurationSection(path);

            if (section == null || !(section.get("low") instanceof Number) || !(section.get("high") instanceof Number)) {
                return new Amount(defaultValue);
            }

            double low = section.getDouble("low");
            double high = section.getDouble("high");

            return new Amount(low, high);
        } else {
            String data = root.getString(path);

            if (Strings.isNullOrEmpty(data)) {
                return new Amount(defaultValue);
            }

            Matcher matcher = DASH_PATTERN.matcher(data);
            if (matcher.matches()) {
                double low = ParseUtil.nullOrDefault(() -> Double.parseDouble(matcher.group(1)),
                        defaultValue,
                        e -> log.warning("Could not parse low amount at " + root.getCurrentPath() + "." + path));

                double high = ParseUtil.nullOrDefault(() -> Double.parseDouble(matcher.group(2)),
                        defaultValue,
                        e -> log.warning("Could not parse high amount at " + root.getCurrentPath() + "." + path));
                return new Amount(low, high);
            }

            // Fixed value syntax
            double fixed = ParseUtil.nullOrDefault(() -> root.getDouble(path),
                    defaultValue,
                    e -> log.warning("Could not parse fixed value amount at " + root.getCurrentPath() + "." + path));
            return new Amount(fixed);
        }
    }

    public int getInt() {
        return fixed ? (int) fixedValue
                : Math.max(BlockRegen.getInstance().getRandom().nextInt((int) highValue + 1), (int) lowValue);
    }

    public double getDouble() {
        return fixed ? fixedValue : Math.max(BlockRegen.getInstance().getRandom().nextDouble() * highValue, lowValue);
    }

    public String toString() {
        return fixed ? String.valueOf(fixedValue) : lowValue + " - " + highValue;
    }
}