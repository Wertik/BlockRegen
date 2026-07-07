package nl.aurorion.blockregen;

import org.jetbrains.annotations.NotNull;

public class BaseContextKey implements ContextKey {

    private final String key;
    protected BaseContextKey(@NotNull String key) {
        this.key = key;
    }

    @NotNull
    public static BaseContextKey of(@NotNull String key) {
        return new BaseContextKey(key);
    }

    @Override
    public @NotNull String key() {
        return this.key;
    }
}
