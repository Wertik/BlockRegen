package nl.aurorion.blockregen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Context {
    private final Map<String, Object> values;

    Context(Map<String, Object> values) {
        this.values = values;
    }

    @NotNull
    public Map<String, Object> values() {
        return Collections.unmodifiableMap(this.values);
    }

    public static Context of(ContextKey key, Object value) {
        return new Context(new HashMap<String, Object>() {{
            put(key.key(), value);
        }});
    }

    public static Context of(Map<String, Object> values) {
        return new Context(values);
    }

    public static Context empty() {
        return new Context(new HashMap<>());
    }

    public Context with(ContextKey key, Object value) {
        this.values.put(key.key(), value);
        return this;
    }

    public void set(ContextKey key, Object value) {
        this.values.put(key.key(), value);
    }

    public Object get(ContextKey key) {
        return this.values.get(key.key());
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

    public Object mustVar(@NotNull ContextKey key) {
        return must(key, this.values.get(key.key()));
    }

    public <T> T mustVar(@NotNull ContextKey key, Class<T> as) {
        return castOrThrow(this.mustVar(key), as);
    }

    @NotNull
    private static <T> T must(@NotNull ContextKey key, T var) {
        return Objects.requireNonNull(var, "Missing key '" + key + "'.");
    }

    public <T> T get(@NotNull ContextKey key, Class<T> clazz) {
        return castOrNull(this.values.get(key.key()), clazz);
    }
}
