package nl.aurorion.blockregen.conditional;

import lombok.Getter;
import nl.aurorion.blockregen.Context;
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
    private String defaultPretty;

    ComposedCondition(ConditionRelation relation, Condition... conditions) {
        this.relation = relation;
        this.conditions.addAll(Arrays.asList(conditions));

        this.defaultAlias = createAlias();
        this.defaultPretty = createPretty();
    }

    ComposedCondition(ConditionRelation relation, List<Condition> conditions) {
        this.relation = relation;
        this.conditions.addAll(conditions);

        this.defaultAlias = createAlias();
        this.defaultPretty = createPretty();
    }

    public void append(Condition condition) {
        conditions.add(condition);

        this.defaultAlias = createAlias();
        this.defaultPretty = createPretty();
    }

    @Override
    public boolean match(Context context) {
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

    @Override
    @NotNull
    public String pretty() {
        return this.getPretty() == null ? this.defaultPretty : this.getPretty();
    }

    private String createAlias() {
        String c = this.conditions.stream()
                .map(Condition::alias)
                .collect(Collectors.joining(" " + relation.toString().toLowerCase() + " "));
        return this.conditions.size() < 2 ? c : "(" + c + ")";
    }

    private String createPretty() {
        return this.conditions.stream()
                .map(Condition::pretty)
                .collect(Collectors.joining(" " + relation.toString().toLowerCase() + " "));
    }
}
