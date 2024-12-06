package nl.aurorion.blockregen.raincloud.flag;

import lombok.experimental.SuperBuilder;
import nl.aurorion.blockregen.raincloud.argument.ValueCommandArgument;

@SuperBuilder
public class ValueCommandFlag<V> extends ValueCommandArgument<V> implements CommandFlag {

    @Override
    public boolean check(String str, boolean allowIncomplete) {
        return this.main.equalsIgnoreCase(str) || (allowIncomplete && this.main.toLowerCase().startsWith(str.toLowerCase()));
    }

    @Override
    public String syntax() {
        return "[--" + this.main + " <value>]";
    }
}
