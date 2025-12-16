package nl.aurorion.blockregen.util;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class Closer implements AutoCloseable {

    private final Deque<Closeable> stack = new ArrayDeque<>();

    private Closer() {
    }

    @NotNull
    public static Closer empty() {
        return new Closer();
    }

    @NotNull
    public <T extends Closeable> T register(@NotNull T closeable) {
        this.stack.push(closeable);
        return closeable;
    }

    @NotNull
    public <T extends AutoCloseable> T register(@NotNull T autoCloseable) {
        this.stack.push(() -> {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close.", e);
            }
        });
        return autoCloseable;
    }

    @Override
    public void close() throws IOException {
        while (!stack.isEmpty()) {
            Closeable closeable = stack.pop();
            closeable.close();
        }
    }
}
