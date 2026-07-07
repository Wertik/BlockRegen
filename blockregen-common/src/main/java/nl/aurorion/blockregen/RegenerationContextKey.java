package nl.aurorion.blockregen;

import org.jetbrains.annotations.NotNull;

public class RegenerationContextKey extends BaseContextKey {
    public static final ContextKey PLAYER = BaseContextKey.of("player");
    public static final ContextKey TOOL = BaseContextKey.of("tool");
    public static final ContextKey BLOCK = BaseContextKey.of("block");

    public static final ContextKey PARSER = BaseContextKey.of("parser");

    public RegenerationContextKey(@NotNull String key) {
        super(key);
    }
}
