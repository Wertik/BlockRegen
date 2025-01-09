package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.IndexedServiceProvider;
import nl.aurorion.blockregen.configuration.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Log
public class ConditionServiceProvider extends IndexedServiceProvider<ConditionProvider> {

    @NotNull
    public Condition load(@NotNull Object node, @Nullable String key) {
        if (key == null) {
            throw new ParseException("Attempted to load a base condition from a list.");
        }

        ConditionProvider parser = getService(key);

        if (parser == null) {
            throw new ParseException("Invalid condition '" + key + "'");
        }

        try {
            return parser.load(node, key).alias(key);
        } catch (ParseException e) {
            throw new ParseException("Failed to parse condition '" + key + "': " + e.getMessage());
        }
    }
}
