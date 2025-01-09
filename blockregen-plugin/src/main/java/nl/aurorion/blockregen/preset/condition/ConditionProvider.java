package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import nl.aurorion.blockregen.configuration.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConditionProvider {
    /**
     * @param key the configuration key this is being loaded from
     *
     * @throws ParseException if the parsing fails.
     * */
    @NotNull
    Condition load(@NotNull Object node, @Nullable String key);
}
