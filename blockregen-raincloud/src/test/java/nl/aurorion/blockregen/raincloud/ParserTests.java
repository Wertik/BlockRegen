package nl.aurorion.blockregen.raincloud;

import lombok.extern.java.Log;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log
public class ParserTests {
    private static final CommandManager manager = new CommandManager((sender, key) -> null);

    private static final Logger logger = Logger.getLogger("nl.aurorion.blockregen");

    @BeforeAll
    public static void buildCommands() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINER);
        logger.setLevel(Level.FINER);
        logger.addHandler(handler);

        manager.command("test", "Test a literal.")
                .literal("foo")
                .handler((context) -> {
                    //
                });

        manager.command("test", "Multiple subcommands.")
                .literal("bar")
                .literal("baz")
                .handler((context) -> {
                    //
                });

        manager.command("test", "Multiple subcommands.")
                .literal("bar")
                .literal("foo")
                .handler((context) -> {
                    //
                });

        manager.command("test", "Multiple subcommands with arguments.")
                .literal("math")
                .literal("add")
                .required("num1", "Number one.",
                        ValueParser.stringParser(),
                        (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .required("num2", "Number two.",
                        ValueParser.stringParser(),
                        (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .flag("num3", "Add another number.", ValueParser.stringParser(), (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .handler((context) -> {
                    //
                });

        manager.command("test", "Multiple subcommands with arguments.")
                .literal("math")
                .literal("sub")
                .required("num1", "Number one.",
                        ValueParser.stringParser(),
                        (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .required("num2", "Number two.",
                        ValueParser.stringParser(),
                        (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .optional("num3", "Number three.",
                        ValueParser.stringParser(),
                        (context, args) -> Stream.of(1, 2, 3).map(String::valueOf).collect(Collectors.toList()))
                .handler((context) -> {
                    //
                });
    }

    // Single literal
    @Test
    public void matchesSingleLiteralCommands() {
        String input = "test foo";
        List<Command<?>> commands = manager.matchCommands(input.split(" "), false);
        assertEquals(1, commands.size());
    }

    @Test
    public void matchesMultipleLiteralCommandsWithMissingParts() {
        String input = "test bar";
        List<Command<?>> commands = manager.matchCommands(input.split(" "), true);
        assertEquals(commands.size(), 2);
    }

    @Test
    public void matchesSingleCommandWithArguments() {
        String input = "test math add 1 2";
        List<Command<?>> commands = manager.matchCommands(input.split(" "), false);
        assertEquals(1, commands.size());
    }

    @Test
    public void matchesSingleCommandWithOptionalArguments() {
        String input = "test math sub 1 2";
        List<Command<?>> commands = manager.matchCommands(input.split(" "), false);
        assertEquals(1, commands.size());

        input = "test math sub 1 2 3";
        commands = manager.matchCommands(input.split(" "), false);
        assertEquals(1, commands.size());
    }

    @Test
    public void matchesMultipleCommandsWithArguments() {
        String input = "test math";
        List<Command<?>> commands = manager.matchCommands(input.split(" "), true);
        assertEquals(commands.size(), 2);
    }

    @Test
    public void providesSuggestions() {
        String label = "test";
        String[] args = new String[]{""};
        CommandSender sender = new MockSender();

        List<String> suggestions = manager.provideTabComplete(sender, label, args);
        assert suggestions != null;
        // order does not matter
        assertEquals(newSet("math", "bar", "foo"), new HashSet<>(suggestions));
    }

    @Test
    public void provideSuggestionsAccordingToInput() {
        String label = "test";
        String[] args = new String[]{"ma"};
        CommandSender sender = new MockSender();

        List<String> suggestions = manager.provideTabComplete(sender, label, args);
        assert suggestions != null;
        assertEquals(newSet("math"), new HashSet<>(suggestions));
    }

    @Test
    public void provideSuggestionsForValues() {
        String label = "test";
        String[] args = new String[]{"math", "sub", ""};
        CommandSender sender = new MockSender();

        List<String> suggestions = manager.provideTabComplete(sender, label, args);
        assert suggestions != null;
        assertEquals(newSet("1", "2", "3"), new HashSet<>(suggestions));
    }

    @Test
    public void providesSuggestionsForFlags() {
        String label = "test";
        String[] args = new String[]{"math", "add", "1", "2", ""};
        CommandSender sender = new MockSender();
        List<String> suggestions = manager.provideTabComplete(sender, label, args);
        assert suggestions != null;
        assertEquals(newSet("--num3"), new HashSet<>(suggestions));
    }

    private static <T> Set<T> newSet(T... args) {
        return new HashSet<>(Arrays.asList(args));
    }
}
