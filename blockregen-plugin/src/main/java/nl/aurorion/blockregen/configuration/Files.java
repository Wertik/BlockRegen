package nl.aurorion.blockregen.configuration;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.storage.exception.StorageException;
import nl.aurorion.blockregen.storage.sqlite.SQLiteStorageDriver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Log
public class Files {

    @Getter
    private final ConfigFile settings;
    @Getter
    private final ConfigFile messages;
    @Getter
    private final ConfigFile blockList;

    public Files(BlockRegenPlugin plugin) {
        this.settings = new ConfigFile(plugin, "Settings.yml");
        this.messages = new ConfigFile(plugin, "Messages.yml");
        this.blockList = new ConfigFile(plugin, "Blocklist.yml");
    }

    public static void readResources(@NotNull String path, @NotNull Consumer<Path> consumer) throws StorageException {
        // https://sqlpey.com/java/java-iterate-jar-resources/
        // Get the URI pointing to the resource directory (e.g., /resources)
        URI uri;
        try {
            uri = SQLiteStorageDriver.class.getClassLoader().getResource(path).toURI();
        } catch (URISyntaxException e) {
            throw new StorageException(e);
        }

        log.fine("uri: " + uri);

        Path rootPath;
        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            // If it's a JAR, create a FileSystem for it
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                rootPath = fs.getPath(path);

                log.fine("root path: " + rootPath);

                // Walk the paths within the created file system
                try (Stream<Path> walk = java.nio.file.Files.walk(rootPath, 1)) {
                    walk.forEach((p) -> {
                        log.fine("found path: " + p);
                        consumer.accept(p);
                    });
                } catch (RuntimeException e) {
                    throw new StorageException(e.getCause());
                }
            } catch (IOException e) {
                throw new StorageException(e);
            }
        } else {
            // If it's a regular directory (e.g., running in IDE)
            rootPath = Paths.get(uri);

            try (Stream<Path> walk = java.nio.file.Files.walk(rootPath, 1)) {
                walk.forEach((p) -> {
                    log.fine("found path: " + p);
                    consumer.accept(p);
                });
            } catch (IOException e) {
                throw new StorageException(e);
            } catch (RuntimeException e) {
                throw new StorageException(e.getCause());
            }
        }
    }

    public void load() {
        this.settings.load();
        this.messages.load();
        this.blockList.load();
    }
}