package nl.aurorion.blockregen.conditional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConditionContext {
    private final Map<String, Object> values;

    ConditionContext(Map<String, Object> values) {
        this.values = values;
    }

    @NotNull
    public Map<String, Object> values() {
        return Collections.unmodifiableMap(this.values);
    }

    public static ConditionContext of(String key, Object value) {
        return new ConditionContext(new HashMap<String, Object>() {{
            put(key, value);
        }});
    }

    public static ConditionContext of(Map<String, Object> values) {
        return new ConditionContext(values);
    }

    public static ConditionContext empty() {
        return new ConditionContext(new HashMap<>());
    }

    public ConditionContext with(String key, Object value) {
        this.values.put(key, value);
        return this;
    }

    public void set(String key, Object value) {
        this.values.put(key, value);
    }

    public Object get(String key) {
        return this.values.get(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T castOrNull(@Nullable Object value, Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz");
        return (T) (!clazz.isInstance(value) ? null : value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castOrThrow(Object var, Class<T> as) {
        Objects.requireNonNull(var, "var");
        Objects.requireNonNull(as, "as");
        if (!as.isInstance(var)) {
            throw new ClassCastException("'" + var + "' cannot be cast to " + as.getName() + " (actual type is " + var.getClass().getName() + ")");
        } else {
            return (T) var;
        }
    }

    public Object mustVar(String key) {
        return must(this.values.get(key));
    }

    public <T> T mustVar(String key, Class<T> as) {
        return castOrThrow(this.mustVar(key), as);
    }

    @NotNull
    private static <T> T must(T var) {
        return Objects.requireNonNull(var, "var");
    }

    public <T> T get(String key, Class<T> clazz) {
        return castOrNull(this.values.get(key), clazz);
    }
}
