package nl.aurorion.blockregen.raincloud.argument;

import lombok.experimental.SuperBuilder;
import nl.aurorion.blockregen.raincloud.CommandContext;

@SuperBuilder
public class LiteralCommandArgument extends BaseCommandArgument {

    @Override
    public void process(CommandContext<?> context, String str) {
        //
    }

    @Override
    public boolean check(String str, boolean allowIncomplete) {
        return this.main.equals(str) || (allowIncomplete && this.main.toLowerCase().startsWith(str.toLowerCase()));
    }
}
