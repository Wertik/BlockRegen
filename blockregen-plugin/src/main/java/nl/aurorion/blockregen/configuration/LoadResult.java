package nl.aurorion.blockregen.configuration;

import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

@Log
public class LoadResult<T, E extends Exception> {

    enum State {
        EMPTY, // contains no value or was missing completely
        FULL, // contains a value, could be null
        ERROR, // an error occurred
    }

    private State state;
    private T value;
    private final E exception;

    private LoadResult(T value, E e, State state) {
        this.value = value;
        this.state = state;
        this.exception = e;
    }

    public static <T> LoadResult<T, Exception> tryLoad(ConfigurationSection root, String path, Function<Object, T> runnable) {
        // Ignore missing
        if (root == null || root.get(path) == null) {
            return LoadResult.empty();
        }

        try {
            return LoadResult.of(runnable.apply(root.get(path)));
        } catch (Exception e) {
            log.warning("Failed to load property '" + path + "' (value: '" + root.get(path) + "') of preset '" + root.getName() + "': " + e.getMessage());
            return LoadResult.error(e);
        }
    }

    @NotNull
    public static <T, E extends Exception> LoadResult<T, E> empty() {
        return new LoadResult<>(null, null, State.EMPTY);
    }

    @NotNull
    public static <T, E extends Exception> LoadResult<T, E> of(@Nullable T value) {
        return new LoadResult<>(value, null, State.FULL);
    }

    @NotNull
    public static <T, E extends Exception> LoadResult<T, E> error(@NotNull E e) {
        return new LoadResult<>(null, e, State.ERROR);
    }

    public boolean isEmpty() {
        return state == State.EMPTY;
    }

    public boolean isPresent() {
        return state == State.FULL;
    }

    public boolean isError() {
        return state == State.ERROR;
    }

    // Set the internal value if the state matches.
    @NotNull
    public LoadResult<T, E> setIfState(@NotNull State state, T def) {
        if (this.state == state) {
            this.state = State.FULL;
            this.value = def;
        }
        return this;
    }

    @NotNull
    public LoadResult<T, E> ifError(T def) {
        return setIfState(State.ERROR, def);
    }

    @NotNull
    public LoadResult<T, E> ifEmpty(T def) {
        return setIfState(State.EMPTY, def);
    }

    @NotNull
    public LoadResult<T, E> ifNotFull(T def) {
        if (state != State.FULL) {
            this.state = State.FULL;
            this.value = def;
        }
        return this;
    }

    // Return the default value if the state matches.
    public T getIfState(@NotNull State state, T def) {
        return state == this.state ? def : this.value;
    }

    public T orElseIfError(T def) {
        return getIfState(State.ERROR, def);
    }

    public T orElseIfEmpty(T def) {
        return getIfState(State.EMPTY, def);
    }

    public void apply(Consumer<T> consumer) {
        if (this.state == State.FULL) {
            consumer.accept(this.value);
        }
    }

    public T get() {
        return value;
    }

    public E error() {
        return this.exception;
    }
}
