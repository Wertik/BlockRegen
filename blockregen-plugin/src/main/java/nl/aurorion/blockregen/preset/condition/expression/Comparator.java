package nl.aurorion.blockregen.preset.condition.expression;

import java.util.function.BiFunction;

// How to compare the two operands?
public interface Comparator<T> extends BiFunction<T, T, Boolean> {
}
