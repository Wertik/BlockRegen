package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConditionProvider {
    /**
     * Load a condition from a configuration node. The node could also be a plain value. In that case, the key is null.
     *
     * @param node Configuration node.
     * @param key The key of the node. Can be null if the node is a plain value.
     *
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    Condition load(@Nullable String key, @NotNull Object node);
}
