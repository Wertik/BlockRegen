package nl.aurorion.blockregen.conditional;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ComposedCondition extends Condition {

    private final List<Condition> conditions = new ArrayList<>();
    @Getter
    private final ConditionRelation relation;

    private String defaultAlias;

    ComposedCondition(ConditionRelation relation, Condition... conditions) {
        this.relation = relation;
        this.conditions.addAll(Arrays.asList(conditions));

        this.defaultAlias = createAlias();
    }

    ComposedCondition(ConditionRelation relation, List<Condition> conditions) {
        this.relation = relation;
        this.conditions.addAll(conditions);

        this.defaultAlias = createAlias();
    }

    public void append(Condition condition) {
        conditions.add(condition);
        this.defaultAlias = createAlias();
    }

    @Override
    public boolean match(ConditionContext context) {
        if (relation == ConditionRelation.AND) {
            for (Condition condition : conditions) {
                if (!condition.matches(context)) {
                    return false;
                }
            }
            return true;
        } else if (relation == ConditionRelation.OR) {
            for (Condition condition : conditions) {
                if (condition.matches(context)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    @NotNull
    public String alias() {
        return this.getAlias() == null ? this.defaultAlias : this.getAlias();
    }

    private String createAlias() {
        String c = this.conditions.stream()
                .map(Condition::alias)
                .collect(Collectors.joining(" " + relation.toString().toLowerCase() + " "));
        return this.conditions.size() < 2 ? c : "(" + c + ")";
    }
}
