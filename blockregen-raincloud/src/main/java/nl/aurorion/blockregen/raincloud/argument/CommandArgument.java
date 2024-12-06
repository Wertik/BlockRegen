package nl.aurorion.blockregen.raincloud.argument;

import nl.aurorion.blockregen.raincloud.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CommandArgument {
    // Get the formal name of this argument.
    @NotNull
    String getName();

    @Nullable
    String getDescription();

    @Nullable
    SuggestionProvider getProvider();

    // Return the syntax of this argument.
    String syntax();

    // Process this argument, aka parse values.
    void process(CommandContext<?> context, String str);

    // Check whether the part of the command input matches this arguments pattern.
    boolean check(String str, boolean allowIncomplete);

    boolean isOptional();
}
