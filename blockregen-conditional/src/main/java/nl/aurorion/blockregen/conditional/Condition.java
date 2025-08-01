package nl.aurorion.blockregen.conditional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class Condition {

    private String alias = null;
    private boolean negate = false;

    protected Condition() {
    }

    protected Condition(String alias) {
        this.alias = alias;
    }

    static class Aliases {
        public static final String DEFAULT_VARIABLE = "X";
        public static final String DEFAULT_CONSTANT = "C";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
    }

    @NotNull
    public static Condition of(@NotNull ConditionFunction function) {
        return new Condition() {
            @Override
            public boolean match(ConditionContext context) {
                return function.match(context);
            }
        };
    }

    @NotNull
    public static Condition of(@NotNull ConditionFunction function, String alias) {
        return new Condition(alias) {
            @Override
            public boolean match(ConditionContext context) {
                return function.match(context);
            }
        };
    }

    public static Condition truthy() {
        return constant(true, Aliases.TRUE);
    }

    public static Condition falsy() {
        return constant(false, Aliases.FALSE);
    }

    public static Condition constant(boolean value, String alias) {
        return of(ctx -> value, alias);
    }

    public static Condition constant(boolean value) {
        return of(ctx -> value, Aliases.DEFAULT_CONSTANT);
    }

    public static Condition anyOf(List<Condition> conditions) {
        return new ComposedCondition(ConditionRelation.OR, conditions);
    }

    public static Condition allOf(List<Condition> conditions) {
        return new ComposedCondition(ConditionRelation.AND, conditions);
    }

    public abstract boolean match(ConditionContext context);

    public boolean matches(ConditionContext context) {
        return this.negate ^ this.match(context);
    }

    public Condition and(Condition condition) {
        if (this instanceof ComposedCondition) {
            ComposedCondition composed = (ComposedCondition) this;
            if (composed.getRelation() == ConditionRelation.AND) {
                composed.append(condition);
                return this;
            }
        }
        return new ComposedCondition(ConditionRelation.AND, this, condition);
    }

    public Condition or(Condition condition) {
        if (this instanceof ComposedCondition) {
            ComposedCondition composed = (ComposedCondition) this;
            if (composed.getRelation() == ConditionRelation.OR) {
                composed.append(condition);
                return this;
            }
        }
        return new ComposedCondition(ConditionRelation.OR, this, condition);
    }

    public Condition negate() {
        this.negate = !this.negate;
        return this;
    }

    @Override
    public String toString() {
        return alias();
    }

    @Nullable
    public String getAlias() {
        return this.alias;
    }

    @NotNull
    public String alias() {
        String a = this.alias == null ? Aliases.DEFAULT_VARIABLE : this.alias;
        return this.negate ? "!" + a : a;
    }

    @NotNull
    public Condition alias(String alias) {
        this.alias = alias;
        return this;
    }
}