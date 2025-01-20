package nl.aurorion.blockregen.preset.condition.expression;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Log
public class TypedComparisons {

    private final Map<Class<?>, Comparator<Object>> comparators = new HashMap<>();

    public TypedComparisons() {
    }

    @NotNull
    static <T> TypedComparisons of(@NotNull Class<T> clazz, @NotNull Comparator<T> cmp) {
        TypedComparisons comparisons = new TypedComparisons();
        comparisons.add(clazz, cmp);
        return comparisons;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    <T> TypedComparisons add(@NotNull Class<T> clazz, @NotNull Comparator<T> cmp) {
        this.comparators.put(clazz, (Comparator<Object>) cmp);
        return this;
    }

    /**
     * @throws ParseException If the parsing fails.
     * */
    boolean parse(@NotNull Object o1, @NotNull Object o2) {
        for (Map.Entry<Class<?>, Comparator<Object>> entry : comparators.entrySet()) {
            Class<?> clazz = entry.getKey();
            Comparator<Object> cmp = entry.getValue();

            if (clazz.isAssignableFrom(o1.getClass()) && clazz.isAssignableFrom(o2.getClass())) {
                log.fine(() -> clazz + " " + o1 + " (" + o1.getClass() + ") " + o2 + " (" + o2.getClass() + ")");
                return cmp.apply(o1, o2);
            }
        }

        throw new ParseException("Values " + o1 + " (" + o1.getClass().getSimpleName() + ") and " + o2 + " (" + o2.getClass().getSimpleName() + ") are not comparable.");
    }
}
