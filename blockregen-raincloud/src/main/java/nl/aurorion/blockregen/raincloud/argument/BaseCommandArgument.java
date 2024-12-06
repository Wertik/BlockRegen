package nl.aurorion.blockregen.raincloud.argument;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import nl.aurorion.blockregen.raincloud.CommandComponent;
import nl.aurorion.blockregen.raincloud.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@SuperBuilder
public abstract class BaseCommandArgument extends CommandComponent implements CommandArgument {

    @Nullable
    protected SuggestionProvider provider;

    protected boolean optional;

    @Override
    public abstract void process(CommandContext<?> context, String str);

    @Override
    public abstract boolean check(String str, boolean allowIncomplete);

    @Override
    public @NotNull String getName() {
        return this.main;
    }

    @Override
    public String syntax() {
        return this.main;
    }
}
