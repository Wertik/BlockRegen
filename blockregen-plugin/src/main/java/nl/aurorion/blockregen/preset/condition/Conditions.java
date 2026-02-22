package nl.aurorion.blockregen.preset.condition;

import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.Context;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for all things condition.
 */
public class Conditions {

    /**
     * Load a condition from a yaml node. This node can either be a list, map, Bukkit ConfigurationSection or a pure
     * node containing a value.
     *
     * @param node     YAML configuration node.
     * @param relation Relation to apply to immediately loaded conditions. This relation does not apply to further
     *                 stacked conditions.
     * @param parser   How to parse conditions from nodes.
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static Condition fromNode(@NotNull Object node, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
        if (node instanceof List) {
            return Conditions.fromList((List<?>) node, relation, parser);
        } else if (node instanceof Map) {
            return Conditions.fromMap((Map<String, Object>) node, relation, parser);
        } else if (node instanceof ConfigurationSection) {
            return Conditions.fromMap(((ConfigurationSection) node).getValues(false), relation, parser);
        }
        return parser.load(null, node);
    }

    /**
     * Load a condition from a yaml node. This node can either be a list, map or a Bukkit ConfigurationSection. Any
     * other type of object is rejected with a {@link ParseException}.
     *
     * @param node     YAML configuration node.
     * @param relation Relation to apply to immediately loaded conditions. This relation does not apply to further
     *                 stacked conditions.
     * @param parser   How to parse conditions from nodes.
     * @throws ParseException If the parsing fails or if a pure node (only value) is provided.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static Condition fromNodeMultiple(@NotNull Object node, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
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

    // Load composed condition from a list
    @SuppressWarnings("unchecked")
    @NotNull
    public static Condition fromList(@NotNull List<?> nodes, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
        List<Condition> conditions = new ArrayList<>();

        for (Object node : nodes) {
            Condition condition;

            if (node instanceof Map) {
                Map<String, Object> values = (Map<String, Object>) node;
                condition = Conditions.fromMap(values, ConditionRelation.AND, parser);
            } else {
                condition = parser.load(null, node);
            }

            conditions.add(condition);
        }

        return relation == ConditionRelation.OR ? Condition.anyOf(conditions) : Condition.allOf(conditions);
    }

    @NotNull
    public static Condition fromMap(@NotNull Map<String, Object> values, @NotNull ConditionRelation relation, @NotNull ConditionProvider parser) {
        List<Condition> conditions = new ArrayList<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Condition condition;
            boolean negate = false;

            String key = entry.getKey();

            // Negation
            if (key.startsWith("^")) {
                key = key.substring(1);
                negate = true;
            }

            if (key.equalsIgnoreCase("all") || key.equalsIgnoreCase("any")) {
                if (!(entry.getValue() instanceof List)) {
                    throw new ParseException("Invalid entry for all/any section.");
                }

                // Parse a stacked condition
                List<?> stackedNodes = (List<?>) entry.getValue();

                condition = Conditions.fromList(stackedNodes,
                        key.equalsIgnoreCase("any") ? ConditionRelation.OR : ConditionRelation.AND,
                        parser);
            } else {
                condition = parser.load(key, entry.getValue());
            }

            if (negate) {
                condition = condition.negate();
            }

            conditions.add(condition);
        }

        return relation == ConditionRelation.OR ? Condition.anyOf(conditions) : Condition.allOf(conditions);
    }

    /**
     * Merge multiple condition contexts. Due to the unmodifiable nature of ConditionContext this operation needs to be
     * done in a copy fashion.
     * <p>
     * Contexts that come later have preference in case of key conflict.
     *
     * @param contexts Contexts to merge.
     * @return A new ConditionContext containing the merged variables.
     */
    @NotNull
    public static Context mergeContexts(Context... contexts) {
        Map<String, Object> result = new HashMap<>();

        for (Context context : contexts) {
            Map<String, Object> vars = context.values();
            result.putAll(vars);
        }
        return Context.of(result);
    }

    /**
     * Use {@link ConditionWrapper} to wrap a condition.
     *
     * @param condition Condition to wrap.
     * @param extender  Extender to call before the condition is matched.
     * @return ConditionWrapper wrapping the condition.
     */
    @NotNull
    public static ConditionWrapper wrap(@NotNull Condition condition, @NotNull ContextExtender extender) {
        return new ConditionWrapper(condition, extender);
    }
}