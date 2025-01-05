package nl.aurorion.blockregen;

import nl.aurorion.blockregen.configuration.ParseException;
import nl.aurorion.blockregen.system.preset.FixedNumberValue;
import nl.aurorion.blockregen.system.preset.NumberValue;
import nl.aurorion.blockregen.system.preset.UniformNumberValue;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class NumberValueTests {

    private static final Logger logger = Logger.getLogger("nl.aurorion.blockregen");

    @BeforeAll
    public static void before() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINER);
        logger.setLevel(Level.FINER);
        logger.addHandler(handler);
    }

    private static NumberValue loadAndParse(String input) {
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new StringReader(input));
        return NumberValue.Parser.load(Objects.requireNonNull(configuration.get("amount")));
    }

    @Test
    public void fixedAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: 1");

        assertInstanceOf(FixedNumberValue.class, value);
        assertEquals(1.0, value.getDouble(), 0.1);
    }

    @Test
    public void fixedAmountsRoundCorrectly() {
        NumberValue value = loadAndParse("amount: 1.2");

        assertInstanceOf(FixedNumberValue.class, value);
        assertEquals(1.2, value.getDouble(), 0.1);
        assertEquals(1, value.getInt());
    }

    @Test
    public void dynamicAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: 1-3");

        assertInstanceOf(UniformNumberValue.class, value);
        UniformNumberValue uniform = (UniformNumberValue) value;
        assertEquals(1.0, uniform.getLow(), 0.1);
        assertEquals(3.0, uniform.getHigh(), 0.1);
    }

    @Test
    public void sectionedDynamicAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount:\n  low: 1\n  high: 3");

        assertInstanceOf(UniformNumberValue.class, value);
        UniformNumberValue uniform = (UniformNumberValue) value;
        assertEquals(1.0, uniform.getLow(), 0.1);
        assertEquals(3.0, uniform.getHigh(), 0.1);
    }

    @Test
    public void negativeFixedAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: -1");

        assertInstanceOf(FixedNumberValue.class, value);
        assertEquals(-1.0, value.getDouble(), 0.1);
    }

    @Test
    public void negativeLowDynamicAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: -1-3");

        assertInstanceOf(UniformNumberValue.class, value);
        UniformNumberValue uniform = (UniformNumberValue) value;
        assertEquals(-1.0, uniform.getLow(), 0.1);
        assertEquals(3.0, uniform.getHigh(), 0.1);
    }

    @Test
    public void negativeHighDynamicAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: -3-1");

        assertInstanceOf(UniformNumberValue.class, value);
        UniformNumberValue uniform = (UniformNumberValue) value;
        assertEquals(-3.0, uniform.getLow(), 0.1);
        assertEquals(1.0, uniform.getHigh(), 0.1);
    }

    @Test
    public void negativeDynamicAmountsLoadCorrectly() {
        NumberValue value = loadAndParse("amount: -3--1");

        assertInstanceOf(UniformNumberValue.class, value);
        UniformNumberValue uniform = (UniformNumberValue) value;
        assertEquals(-3.0, uniform.getLow(), 0.1);
        assertEquals(-1.0, uniform.getHigh(), 0.1);
    }

    @Test
    public void uniformValuesGetReordered() {
        UniformNumberValue uniform = NumberValue.uniform(4, 1);
        assertEquals(1, uniform.getLow(), 0.1);
        assertEquals(4, uniform.getHigh(), 0.1);
    }

    @Test
    public void throwsWithFixedNonNumberValues() {
        assertThrows(ParseException.class, () -> loadAndParse("amount: nine"));
    }

    @Test
    public void throwsWithDynamicNonNumberValues() {
        assertThrows(ParseException.class, () -> loadAndParse("amount: nine-2"));
    }

    @Test
    public void throwsWithSectionedDynamicNonNumberValues() {
        assertThrows(ParseException.class, () -> loadAndParse("amount:\n  low: nine\n  high: ten"));
    }

    @Test
    public void throwsWithInvalidSections() {
        assertThrows(ParseException.class, () -> loadAndParse("amount:\n low: 2\n not-high: 10"));
    }
}
