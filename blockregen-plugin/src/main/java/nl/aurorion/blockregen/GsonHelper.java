package nl.aurorion.blockregen;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Gson (json) save and load helper class.
 * <p>
 * Copied from DevportUtils by devport.space
 *
 * @author qwz
 */
@Log
public class GsonHelper {

    @Getter
    private final Gson gson;

    public GsonHelper(GsonBuilder gsonBuilder) {
        this.gson = gsonBuilder.create();
    }

    public static <T> Type mapList(@NotNull Class<T> innerType) {
        return TypeToken.getParameterized(List.class, innerType).getType();
    }

    public static <T> Type map(@NotNull Class<T> clazz) {
        return new TypeToken<T>() {
        }.getType();
    }

    /**
     * Asynchronously read ByteBuffer from a file.
     */
    @NotNull
    public CompletableFuture<ByteBuffer> read(@NotNull final Path path) {

        AsynchronousFileChannel channel;
        long size;
        try {
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
            size = channel.size();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not open an asynchronous file channel.", e);
            return CompletableFuture.supplyAsync(() -> {
                throw new CompletionException(e);
            });
        }

        if (size > Integer.MAX_VALUE) {
            return CompletableFuture.supplyAsync(() -> {
                throw new CompletionException(new IllegalStateException("File is too big for the reader."));
            });
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) size);

        CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
        channel.read(buffer, 0, future, new CompletionHandler<Integer, CompletableFuture<ByteBuffer>>() {
            @Override
            public void completed(Integer result, CompletableFuture<ByteBuffer> attachment) {
                future.complete(buffer);
            }

            @Override
            public void failed(Throwable exc, CompletableFuture<ByteBuffer> attachment) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    /**
     * Load and parse json from a file.
     *
     * @return Parsed output or null.
     */
    @Nullable
    public <T> T load(@NotNull String dataPath, @NotNull Type type) {

        Path path = Paths.get(dataPath);

        if (!Files.exists(path))
            return null;

        String input;
        try {
            input = String.join("", Files.readAllLines(path));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not read JSON file: " + e.getMessage(), e);
            return null;
        }

        if (Strings.isNullOrEmpty(input))
            return null;

        return gson.fromJson(input, type);
    }

    /**
     * Asynchronously load a List<T> from a file.
     *
     * @return CompletableFuture with the resulting list or null.
     */
    @NotNull
    public <T> CompletableFuture<List<T>> loadListAsync(@NotNull final String dataPath, @NotNull Class<T> innerClazz) {
        Path path = Paths.get(dataPath);

        if (!Files.exists(path))
            return new CompletableFuture<>();

        final Type type = mapList(innerClazz);

        return read(path).thenApplyAsync(buffer -> {
            String output = new String(buffer.array(), StandardCharsets.UTF_8).trim();

            if (Strings.isNullOrEmpty(output))
                return null;

            return gson.fromJson(output, type);
        });
    }

    /**
     * Asynchronously save data to json.
     *
     * @return CompletableFuture with the number of bytes written
     */
    @NotNull
    public <T> CompletableFuture<Void> save(@NotNull final T input, @NotNull final Path dataPath) {
        final Type type = map(input.getClass());

        return CompletableFuture.runAsync(() -> {
            String jsonString = gson.toJson(input, type).trim();

            try {
                Files.write(dataPath, jsonString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}