package nl.aurorion.blockregen.preset.condition.expression;

import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;

@Log
public class TypedComparisons {

    private final Map<Class<?>, Comparator<Object>> m = new HashMap<>();

    public TypedComparisons() {
    }

    static <T> TypedComparisons of(Class<T> clazz, Comparator<T> cmp) {
        TypedComparisons comparisons = new TypedComparisons();
        comparisons.add(clazz, cmp);
        return comparisons;
    }

    @SuppressWarnings("unchecked")
    <T> TypedComparisons add(Class<T> clazz, Comparator<T> cmp) {
        this.m.put(clazz, (Comparator<Object>) cmp);
        return this;
    }

    boolean parse(Object o1, Object o2) {
        for (Map.Entry<Class<?>, Comparator<Object>> entry : m.entrySet()) {
            Class<?> clazz = entry.getKey();
            Comparator<Object> cmp = entry.getValue();

            log.fine(() -> clazz + " " + o1 + " (" + o1.getClass() + ") " + o2 + " (" + o2.getClass() + ")");
            if (clazz.isAssignableFrom(o1.getClass()) && clazz.isAssignableFrom(o2.getClass())) {
                return cmp.apply(o1, o2);
            }
        }
        return false;
    }
}
