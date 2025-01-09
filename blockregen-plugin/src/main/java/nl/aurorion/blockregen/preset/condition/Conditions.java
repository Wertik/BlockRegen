package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import nl.aurorion.blockregen.configuration.ParseException;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conditions {

    @SuppressWarnings("unchecked")
    public static Condition fromNodeMultiple(Object node, ConditionRelation relation, ConditionProvider parser) {
        if (node instanceof List) {
            return Conditions.fromList((List<?>) node, relation, parser);
        } else if (node instanceof Map) {
            return Conditions.fromMap((Map<String, Object>) node, relation, parser);
        } else if (node instanceof ConfigurationSection) {
            return Conditions.fromMap(((ConfigurationSection) node).getValues(false), relation, parser);
        } else {
            throw new ParseException("Node cannot be loaded from a single value.");
        }
    }

    @SuppressWarnings("unchecked")
    public static Condition fromNode(Object node, ConditionRelation relation, ConditionProvider parser) {
        if (node instanceof List) {
            return Conditions.fromList((List<?>) node, relation, parser);
        } else if (node instanceof Map) {
            return Conditions.fromMap((Map<String, Object>) node, relation, parser);
        }
        return parser.load(node, null);
    }

    // Load composed condition from a list
    @SuppressWarnings("unchecked")
    @NotNull
    public static Condition fromList(@NotNull List<?> nodes, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
        Condition baseCondition = relation == ConditionRelation.OR ? Condition.falseCondition() : Condition.trueCondition();

        for (Object node : nodes) {
            Condition condition;

            if (node instanceof Map) {
                Map<String, Object> values = (Map<String, Object>) node;
                condition = Conditions.fromMap(values, ConditionRelation.AND, parser);
            } else {
                condition = parser.load(node, null);
            }

            if (relation == ConditionRelation.OR) {
                baseCondition = baseCondition.or(condition);
            } else {
                baseCondition = baseCondition.and(condition);
            }
        }

        return baseCondition;
    }

    @NotNull
    public static Condition fromMap(@NotNull Map<String, Object> values, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
        Condition sectionCondition = relation == ConditionRelation.OR ? Condition.falseCondition() : Condition.trueCondition();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Condition condition;
            boolean negate = false;

            String key = entry.getKey();

            // Negation
            if (key.startsWith("^")) {
                key = key.substring(1);
                negate = true;
            }

            if (!key.equalsIgnoreCase("all") && !key.equalsIgnoreCase("any")) {
                condition = parser.load(entry.getValue(), key);
            } else {
                if (!(entry.getValue() instanceof List)) {
                    throw new ParseException("Invalid entry for all/any section.");
                }

                // Parse a stacked condition
                List<?> stackedNodes = (List<?>) entry.getValue();

                condition = Conditions.fromList(stackedNodes,
                        key.equalsIgnoreCase("any") ? ConditionRelation.OR : ConditionRelation.AND,
                        parser);
            }

            if (negate) {
                condition = condition.negate();
            }

            if (relation == ConditionRelation.OR) {
                sectionCondition = sectionCondition.or(condition);
            } else {
                sectionCondition = sectionCondition.and(condition);
            }
        }

        return sectionCondition;
    }

    /**
     * Merge multiple condition contexts. Due to the unmodifiable nature of ConditionContext this operation needs to be done in a copy fashion.
     * <p>
     * Contexts that come later have preference in case of key conflict.
     * */
    @NotNull
    public static ConditionContext mergeContexts(ConditionContext... contexts) {
        Map<String, Object> result = new HashMap<>();

        for (ConditionContext context : contexts) {
            Map<String, Object> vars = context.contextVariables();
            result.putAll(vars);
        }
        return ConditionContext.of(result);
    }

    @NotNull
    public static ConditionWrapper wrap(@NotNull Condition condition, @NotNull ContextExtender extender) {
        return new ConditionWrapper(condition, extender);
    }
}