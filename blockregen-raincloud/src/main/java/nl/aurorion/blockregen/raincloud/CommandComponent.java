package nl.aurorion.blockregen.raincloud;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;

@Getter
@SuperBuilder
public abstract class CommandComponent {

    protected final String main;
    protected String[] aliases;

    @Nullable
    protected String description;

    public CommandComponent(String main, String[] aliases, @Nullable String description) {
        this.main = main;
        this.aliases = aliases;
        this.description = description;
    }

    public CommandComponent(CommandComponent component) {
        this.main = component.main;
        this.aliases = component.aliases;
        this.description = component.description;
    }

    @Override
    public String toString() {
        return main;
    }
}
