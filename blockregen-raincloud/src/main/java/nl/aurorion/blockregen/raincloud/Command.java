package nl.aurorion.blockregen.raincloud;

import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.raincloud.argument.CommandArgument;
import nl.aurorion.blockregen.raincloud.argument.LiteralCommandArgument;
import nl.aurorion.blockregen.raincloud.argument.SuggestionProvider;
import nl.aurorion.blockregen.raincloud.argument.ValueCommandArgument;
import nl.aurorion.blockregen.raincloud.flag.CommandFlag;
import nl.aurorion.blockregen.raincloud.flag.CommandSwitch;
import nl.aurorion.blockregen.raincloud.flag.ValueCommandFlag;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
@Log
public class Command<C extends CommandSender> extends CommandComponent {

    public interface CommandHandler<C extends CommandSender> {
        void handle(CommandContext<C> context);
    }

    public interface ExceptionHandler<C extends CommandSender, E extends Exception> {
        void handle(CommandContext<C> context, E e);
    }

    private final CommandManager manager;

    private final List<CommandArgument> arguments;

    private final List<CommandFlag> flags;

    private CommandHandler<C> handler;

    private ExceptionHandler<C, ParseException> parseExceptionHandler;

    @Getter
    private String permission;

    private TypeToken<C> senderType = (TypeToken<C>) TypeToken.of(CommandSender.class);

    public Command(String main, @Nullable String[] aliases, @Nullable String description, CommandManager manager) {
        super(main, aliases, description);
        this.manager = manager;
        this.flags = new ArrayList<>();
        this.arguments = new ArrayList<>();
    }

    public Command(CommandComponent commandComponent, CommandManager manager, List<CommandArgument> arguments, List<CommandFlag> flags, CommandHandler<C> handler, ExceptionHandler<C, ParseException> parseExceptionHandler, TypeToken<C> senderType) {
        super(commandComponent);
        this.parseExceptionHandler = parseExceptionHandler;
        this.manager = manager;
        this.arguments = arguments;
        this.handler = handler;
        this.flags = flags;
        this.senderType = senderType;
    }

    public Command<C> argument(CommandArgument argument) {
        this.arguments.add(argument);
        return this;
    }

    public Command<C> literal(String literal) {
        return this.argument(LiteralCommandArgument.builder().main(literal)
                .provider((context, args) -> new ArrayList<>(Collections.singleton(literal)))
                .build());
    }

    public Command<C> literal(String literal, String... aliases) {
        return this.argument(LiteralCommandArgument.builder()
                .main(literal)
                .aliases(aliases)
                .provider((context, args) -> {
                    List<String> suggestions = new ArrayList<>(Arrays.asList(aliases));
                    suggestions.add(literal);
                    return suggestions;
                })
                .build());
    }

    public <V extends @NonNull Object> Command<C> required(String main, ValueParser<V> parser) {
        return this.argument(ValueCommandArgument.<V>builder()
                .main(main)
                .parser(parser)
                .build());
    }

    public <V extends @NonNull Object> Command<C> required(String main, String description, ValueParser<V> parser, SuggestionProvider provider) {
        return this.argument(ValueCommandArgument.<V>builder()
                .main(main)
                .description(description)
                .parser(parser)
                .provider(provider)
                .build());
    }

    public <V extends @NonNull Object> Command<C> optional(String main, ValueParser<V> parser) {
        return this.argument(ValueCommandArgument.<V>builder()
                .main(main)
                .optional(true)
                .parser(parser)
                .build());
    }

    public <V extends @NonNull Object> Command<C> optional(String main, String description, ValueParser<V> parser, SuggestionProvider provider) {
        return this.argument(ValueCommandArgument.<V>builder()
                .optional(true)
                .main(main)
                .parser(parser)
                .provider(provider)
                .description(description)
                .build());
    }

    public Command<C> flag(CommandFlag flag) {
        this.flags.add(flag);
        return this;
    }

    public <V extends @NonNull Object> Command<C> flag(String main, String description, ValueParser<V> parser, SuggestionProvider provider) {
        return this.flag(ValueCommandFlag.<V>builder()
                .main(main)
                .optional(true)
                .description(description)
                .parser(parser)
                .provider(provider)
                .build());
    }

    public Command<C> switchFlag(String main, String description) {
        return this.flag(CommandSwitch.builder()
                .main(main)
                .optional(true)
                .description(description)
                .build());
    }

    public void handler(CommandHandler<C> handler) {
        this.handler = (context) -> {
            // Check sender type
            C sender = context.sender();
            if (!this.senderType.isSupertypeOf(TypeToken.of(sender.getClass()))) {
                sender.sendMessage(this.manager.getLanguage().get(sender, "Only-Players"));
                return;
            }

            // Permissions
            if (permission != null && !context.sender().hasPermission(permission)) {
                sender.sendMessage(this.manager.getLanguage().get(sender, "Insufficient-Permission"));
                return;
            }
            handler.handle(context);
        };
        this.manager.registerCommand(this);
    }

    public Command<C> onParseException(ExceptionHandler<C, ParseException> handler) {
        this.parseExceptionHandler = handler;
        return this;
    }

    @SuppressWarnings({"unchecked"})
    public Command<Player> senderPlayer() {
        return new Command<>(this, this.manager, this.arguments, this.flags, (CommandHandler<Player>) this.handler, (ExceptionHandler<Player, ParseException>) parseExceptionHandler, TypeToken.of(Player.class));
    }

    public Command<C> permission(@NotNull String permission) {
        this.permission = permission;
        return this;
    }

    public boolean check(String[] parts) {
        return check(parts, false);
    }

    public boolean check(String[] parts, boolean allowIncomplete) {
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        // Argument index
        int i = 0;

        Iterator<String> partIt = Arrays.stream(args).iterator();

        // Check the command arguments and flags
        while (partIt.hasNext()) {
            String part = partIt.next();

            if (part.startsWith("--") || part.startsWith("-")) {
                String name = part.replace('-', ' ').trim();

                CommandFlag flag = this.getFlag(name);

                if (flag == null) {
                    // Doesn't have the flag.
                    return false;
                }

                // flags with values have to skip the next part
                if (flag instanceof ValueCommandFlag<?> && partIt.hasNext()) {
                    partIt.next();
                }

                // good
                continue;
            }

            if (this.getArguments().size() <= i) {
                return false;
            }

            // Check next argument
            CommandArgument argument = this.getArguments().get(i);
            i++;

            if (!argument.check(part, allowIncomplete)) {
                return false;
            }
        }

        log.fine(i + " < " + (arguments.size() - arguments.stream().filter(CommandArgument::isOptional).count()));

        if (!allowIncomplete && i < arguments.size() - arguments.stream().filter(CommandArgument::isOptional).count()) {
            log.fine("Not enough arguments for command " + this);
            return false;
        }

        log.fine("Command " + this + " matches " + String.join(" ", parts));
        return true;
    }

    public String syntax() {
        return this.syntax(this.main);
    }

    public String syntax(String label) {
        StringBuilder builder = new StringBuilder("/" + label);
        for (CommandArgument argument : this.arguments) {
            builder.append(" ").append(argument.syntax());
        }
        for (CommandFlag flag : this.flags) {
            builder.append(" ").append(flag.syntax());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.main);
        for (CommandArgument argument : this.arguments) {
            builder.append(" ").append(argument.syntax());
        }
        return builder.toString();
    }

    public void call(CommandContext<C> context, String[] parts) {
        this.process(context, parts);
        this.handler.handle(context);
    }

    public void process(CommandContext<C> context, String[] parts) {
        process(context, parts, false);
    }

    public void process(CommandContext<C> context, String[] parts, boolean silent) {
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        log.fine("Processing command " + this);

        // Argument index
        int i = 0;

        Iterator<String> it = Arrays.stream(args).iterator();
        CommandArgument argument = null;

        // Check the command arguments and flags
        while (it.hasNext()) {
            try {
                String part = it.next();

                log.fine("Part " + part);

                if (part.startsWith("--") || part.startsWith("-")) {
                    String name = part.replace('-', ' ').trim();

                    CommandFlag flag = this.getFlag(name);

                    if (flag instanceof ValueCommandFlag<?>) {
                        argument = flag;
                        if (it.hasNext()) {
                            flag.process(context, it.next());
                        }
                        log.fine("Value flag " + flag);
                    } else if (flag instanceof CommandSwitch) {
                        CommandSwitch commandSwitch = (CommandSwitch) flag;
                        argument = commandSwitch;
                        commandSwitch.process(context, part);
                        log.fine("Switch flag " + commandSwitch);
                    } else if (flag == null) {
                        log.fine("Command " + this + " doesn't allow the flag " + name);
                        return;
                    }

                    // good
                    continue;
                }

                log.fine("Argument " + i);

                // Check next argument
                argument = this.getArguments().get(i);
                i++;

                argument.process(context, part);
            } catch (ParseException e) {
                if (silent) {
                    return;
                }

                if (this.parseExceptionHandler != null) {
                    this.parseExceptionHandler.handle(context, new ParseException(Objects.requireNonNull(argument).getName(), e.getValue(), e.getMessage()));
                }
                log.fine("ParseException for command " + this + ": " + e.getMessage());
                return;
            }
        }
    }

    public List<CommandArgument> getArguments() {
        return Collections.unmodifiableList(this.arguments);
    }

    public List<CommandFlag> getFlags() {
        return Collections.unmodifiableList(this.flags);
    }

    public CommandFlag getFlag(String name) {
        return this.flags.stream().filter(flag -> Objects.equals(flag.getName(), name)).findAny().orElse(null);
    }
}
