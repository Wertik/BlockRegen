package nl.aurorion.blockregen.preset.condition.expression;

import lombok.Getter;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.util.Objects;

public enum OperandRelation {
    GTE(">=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() >= num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> t1.equals(t2) || t1.isAfter(t2))),
    LTE("<=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() <= num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> t1.equals(t2) || t1.isBefore(t2))),
    EQ("==", TypedComparisons
            .of(Number.class, Objects::equals)
            .add(LocalTime.class, Objects::equals)),
    NEQ("!=", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() != num2.doubleValue())
            .add(LocalTime.class, (t1, t2) -> !Objects.equals(t1, t2))),
    LT("<", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() < num2.doubleValue())
            .add(LocalTime.class, LocalTime::isBefore)),
    GT(">", TypedComparisons
            .of(Number.class, (num1, num2) -> num1.doubleValue() > num2.doubleValue())
            .add(LocalTime.class, LocalTime::isAfter));

    @Getter
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

    /**
     * @throws ParseException If the parsing fails.
     */
    boolean evaluate(@NotNull Object o1, @NotNull Object o2) {
        return this.comparisons.parse(o1, o2);
    }

    @Override
    public String toString() {
        return "OperandRelation{" +
                "symbol='" + symbol + '\'' +
                '}';
    }
}
