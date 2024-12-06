package nl.aurorion.blockregen.raincloud.argument;

import lombok.experimental.SuperBuilder;
import nl.aurorion.blockregen.raincloud.CommandContext;
import nl.aurorion.blockregen.raincloud.ParseException;
import nl.aurorion.blockregen.raincloud.ValueParser;

@SuperBuilder
public class ValueCommandArgument<V> extends BaseCommandArgument {

    private final ValueParser<V> parser;

    @Override
    public boolean check(String str, boolean allowIncomplete) {
        return true;
    }

    @Override
    public void process(CommandContext<?> context, String str) {
        V value;
        try {
            value = parser.parse(str);
        } catch (ParseException e) {
            throw new ParseException(this.main, str, e.getMessage());
        }
        context.add(this.main, value);
    }

    @Override
    public String syntax() {
        return this.isOptional() ? "[" + this.main + "]" : "<" + this.main + ">";
    }
}
