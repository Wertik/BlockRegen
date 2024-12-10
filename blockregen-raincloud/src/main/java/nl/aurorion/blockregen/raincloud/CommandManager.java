package nl.aurorion.blockregen.raincloud;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.raincloud.argument.CommandArgument;
import nl.aurorion.blockregen.raincloud.argument.SuggestionProvider;
import nl.aurorion.blockregen.raincloud.argument.ValueCommandArgument;
import nl.aurorion.blockregen.raincloud.flag.CommandFlag;
import nl.aurorion.blockregen.raincloud.flag.ValueCommandFlag;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log
@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals", "unused"})
public class CommandManager implements CommandExecutor, TabCompleter {

    private final List<Command<? extends CommandSender>> commands = new ArrayList<>();

    @Getter
    private final LanguageProvider language;

    public CommandManager(LanguageProvider language) {
        this.language = language;
    }

    public void registerCommand(Command<? extends CommandSender> command) {
        this.commands.add(command);
    }

    public Command<? extends CommandSender> command(String main, String description) {
        return buildCommand(main, null, description);
    }

    public Command<? extends CommandSender> command(String main, @Nullable String[] aliases, String description) {
        return buildCommand(main, aliases, description);
    }

    private Command<? extends CommandSender> buildCommand(String main, @Nullable String[] aliases, String description) {
        return new Command<>(main, aliases, description, this);
    }

    private String[] prepareCommand(String label, String[] args) {
        String[] parts = new String[args.length + 1];
        parts[0] = label;
        System.arraycopy(args, 0, parts, 1, args.length);
        return parts;
    }

    public List<Command<?>> matchCommands(String[] parts, boolean allowMissing) {
        List<Command<?>> commands = new ArrayList<>(this.commands);
        commands.removeIf(command -> !command.check(parts, allowMissing));
        return commands;
    }

    public <C extends CommandSender> void runCommand(C sender, String label, String[] args) {
        String[] parts = this.prepareCommand(label, args);

        List<Command<?>> commands = this.matchCommands(parts, false);

        if (commands.isEmpty()) {
            List<Command<?>> suggestions = this.matchCommands(parts, true);

            if (suggestions.isEmpty()) {
                return;
            }

            if (suggestions.size() == 1) {
                sender.sendMessage(StringUtil.color(this.specificHelp(suggestions.get(0))));
                return;
            }

            sender.sendMessage(StringUtil.color(this.composeHelp(sender, label, suggestions)));
            return;
        }

        if (commands.size() > 1) {
            // unknown command
            sender.sendMessage(StringUtil.color("&cUnknown command."));
            return;
        }

        Command<C> command = (Command<C>) commands.get(0);

        CommandContext<C> context = new CommandContext<>(command, sender, label, args);
        command.call(context, parts);
    }

    public Collection<Command<? extends CommandSender>> getCommands() {
        return Collections.unmodifiableCollection(this.commands);
    }

    public String composeHelp(CommandSender sender, String label) {
        return this.composeHelp(sender, label, (Predicate<Command<?>>) null);
    }

    public String composeHelp(CommandSender sender, String label, @NotNull List<Command<?>> commands) {
        StringBuilder builder = new StringBuilder();
        for (Command<? extends CommandSender> command : commands) {
            log.fine(command + " " + command.getPermission() + " " + (command.getPermission() != null ? sender.hasPermission(command.getPermission()) : null));
            if (command.getPermission() == null || sender.hasPermission(command.getPermission())) {
                builder.append("&3").append(command.syntax(label)).append(" &8- &7").append(command.getDescription()).append("&r\n");
            }
        }
        return builder.toString().trim();
    }

    public String composeHelp(CommandSender sender, String label, @Nullable Predicate<Command<?>> filter) {
        return this.composeHelp(sender, label, this.commands.stream().filter((c) -> filter == null || filter.test(c)).collect(Collectors.toList()));
    }

    public String help(CommandSender sender, String label, @Nullable String query) {
        if (query == null) {
            return this.composeHelp(sender, label);
        }

        // Find commands that match through regex.

        Pattern pattern = Pattern.compile(String.format("(?i)%s", query));

        List<Command<?>> results = new ArrayList<>();

        for (Command<?> command : this.commands) {
            if (pattern.matcher(command.syntax()).find()) {
                results.add(command);
            }
        }

        if (results.size() == 1) {
            return this.specificHelp(commands.get(0));
        }

        return this.composeHelp(sender, label, results::contains);
    }

    public String specificHelp(Command<? extends CommandSender> command) {
        // Info about arguments
        StringBuilder builder = new StringBuilder("&3" + command.syntax() + "\n");
        for (CommandArgument argument : command.getArguments()) {
            if (argument.getDescription() == null) {
                continue;
            }
            builder.append(String.format("  &3%s &8- &7%s", argument.getName(), argument.getDescription())).append('\n');
        }
        return builder.toString();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        this.runCommand(sender, label, args);
        return false;
    }

    // Provide tab complete suggestions for a specific command.
    private <C extends CommandSender> void provideSuggestions(Command<C> command, C sender, int i, Map<Command<?>, CommandContext<?>> matched, Set<String> suggestions, String[] parts, String label, String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            log.fine("No permissions for command " + command);
            return;
        }

        if (!command.check(parts, true)) {
            return;
        }

        // Fill context with processes arguments for use in providers
        CommandContext<C> context = new CommandContext<>(command, sender, label, args);
        command.process(context, parts, true);

        matched.put(command, context);

        if (command.getArguments().size() > i) {
            SuggestionProvider provider = command.getArguments().get(i).getProvider();

            if (provider != null) {
                suggestions.addAll(provider.provide(context, args));
            }
        }
    }

    public @Nullable List<String> provideTabComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        // Figure out which argument we are at right now, according to that provide suggestions from different commands.
        Set<String> suggestions = new HashSet<>();

        // Currently whispered argument
        int index = args.length;

        log.fine(String.join(",", args));

        String[] parts = new String[args.length + 1];
        parts[0] = label;
        System.arraycopy(args, 0, parts, 1, args.length);

        Map<Command<?>, CommandContext<?>> matched = new HashMap<>();

        for (Command<? extends CommandSender> command : this.commands) {
            provideSuggestions((Command<CommandSender>) command, sender, index - 1, matched, suggestions, parts, label, args);
        }

        // Flags
        if (matched.size() == 1) {
            Command<?> command = matched.keySet().stream().findFirst().orElse(null);

            boolean suggestedValue = false;

            // Add value suggestions for value flags
            if (index > 1) {
                String previous = parts[index - 1];

                if (previous.startsWith("--") || previous.startsWith("-")) {
                    String name = previous.replace('-', ' ').trim();
                    CommandFlag flag = command.getFlag(name);

                    if (flag instanceof ValueCommandFlag<?>) {
                        ValueCommandArgument<?> valueFlag = (ValueCommandArgument<?>) flag;
                        SuggestionProvider provider = valueFlag.getProvider();

                        if (provider != null) {
                            suggestions.addAll(provider.provide(matched.get(command), args));
                            suggestedValue = true;
                        }
                    }
                }
            }

            if (!command.getFlags().isEmpty() && !suggestedValue) {
                suggestions.addAll(command.getFlags().stream().map(flag -> "--" + flag).filter(f -> !String.join(" ", parts).contains(f)).collect(Collectors.toSet()));
            }
        }

        log.fine(parts[index].toLowerCase() + " " + String.join(",", suggestions));

        return suggestions.stream().filter(s -> index == 0 || s.toLowerCase().startsWith(parts[index].toLowerCase())).collect(Collectors.toList());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command bukkitCommand, @NotNull String label, @NotNull String[] args) {
        return this.provideTabComplete(sender, label, args);
    }
}
