package nl.aurorion.blockregen.raincloud.flag;

import lombok.experimental.SuperBuilder;
import nl.aurorion.blockregen.raincloud.CommandContext;
import nl.aurorion.blockregen.raincloud.argument.BaseCommandArgument;

@SuperBuilder
public class CommandSwitch extends BaseCommandArgument implements CommandFlag {

    @Override
    public void process(CommandContext<?> context, String str) {
        context.add(this.main, true);
    }

    @Override
    public boolean check(String str, boolean allowIncomplete) {
        return this.main.equalsIgnoreCase(str) || (allowIncomplete && this.main.toLowerCase().startsWith(str.toLowerCase()));
    }

    @Override
    public String syntax() {
        return "[--" + this.main + "]";
    }
}
