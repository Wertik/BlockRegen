package nl.aurorion.blockregen.preset.condition.expression;

import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.util.Objects;

public enum OperandRelation {
    LT("<", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() < num2.doubleValue())
            .add(LocalTime.class, LocalTime::isBefore)),
    LTE("<=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() <= num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> t1.equals(t2) || t1.isBefore(t2))),
    GT(">", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() > num2.doubleValue())
            .add(LocalTime.class, LocalTime::isAfter)),
    GTE(">=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() >= num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> t1.equals(t2) || t1.isAfter(t2))),
    EQ("==", TypedComparisons
            .of(Number.class, Objects::equals)
            .add(LocalTime.class, Objects::equals)),
    NEQ("!=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() != num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> !Objects.equals(t1, t2)));

    private final String symbol;
    private final TypedComparisons comparisons;

    OperandRelation(String symbol, TypedComparisons comparisons) {
        this.symbol = symbol;
        this.comparisons = comparisons;
    }

    @Nullable
    public static OperandRelation parse(String input) {
        for (OperandRelation relation : OperandRelation.values()) {
            if (relation.symbol.equalsIgnoreCase(input)) {
                return relation;
            }
        }
        return null;
    }

    boolean evaluate(Object o1, Object o2) {
        return this.comparisons.parse(o1, o2);
    }

    @Override
    public String toString() {
        return "OperandRelation{" +
                "symbol='" + symbol + '\'' +
                '}';
    }
}
