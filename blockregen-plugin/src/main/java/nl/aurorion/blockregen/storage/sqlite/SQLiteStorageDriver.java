package nl.aurorion.blockregen.storage.sqlite;

import com.google.common.base.Charsets;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.region.CuboidRegion;
import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import nl.aurorion.blockregen.storage.*;
import nl.aurorion.blockregen.util.Closer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Log
public class SQLiteStorageDriver implements StorageDriver {

    private final static int DEFAULT_TIMEOUT_MILLIS = 5000;

    private final static ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final File file;

    private SQLiteStorageDriver(@NotNull Options options) {
        this.file = options.file();
    }

    public static class Provider implements DriverProvider {

        // Base folder.
        // #Path is relative to this.
        private final File dataDir;

        private final File defaultFile;

        private Provider(File dataDir, File defaultFile) {
            this.dataDir = dataDir;
            this.defaultFile = defaultFile;
        }

        @NotNull
        public static Provider in(@NotNull File dataDir) {
            File defaultFile = new File(dataDir, "blockregen.db");
            return new Provider(dataDir, defaultFile);
        }

        @Override
        public @NotNull StorageDriver create(@Nullable ConfigurationSection section) {
            Options options = Options.empty()
                    .file(this.defaultFile);

            if (section != null) {
                String path = section.getString("Path");

                if (path != null) {
                    File file = new File(this.dataDir, path);
                    options.file(file);
                } else {
                    log.warning(() -> "No path specified for SQLite driver. Assuming 'blockregen.db'.");
                }
            } else {
                log.warning(() -> "No path specified for SQLite driver. Assuming 'blockregen.db'.");
            }

            return SQLiteStorageDriver.with(options);
        }
    }

    @NoArgsConstructor
    public static class Options {
        private File file;

        private Options(Options options) {
            this.file = options.file;
        }

        @NotNull
        public static Options empty() {
            return new Options();
        }

        @NotNull
        public Options file(@NotNull File file) {
            this.file = file;
            return this;
        }

        public File file() {
            return this.file;
        }
    }

    @NotNull
    public static SQLiteStorageDriver with(@NotNull Options options) {
        return new SQLiteStorageDriver(new Options(options));
    }

    private static void readResources(@NotNull String path, @NotNull Consumer<Path> consumer) throws StorageException {
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
                try (Stream<Path> walk = Files.walk(rootPath, 1)) {
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

            try (Stream<Path> walk = Files.walk(rootPath, 1)) {
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

    private static Map<String, String> readMigrations() throws StorageException {
        Map<String, String> migrations = new HashMap<>();

        readResources("sqlite/migrations/", (path) -> {
            if (!path.toString().endsWith(".sql")) {
                return;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(path, Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // use filename without suffix as a name
            String fileName = path.getFileName().toString();
            migrations.put(fileName.substring(0, fileName.length() - 4), String.join("\n", lines));
        });

        return migrations;
    }

    @Override
    public void initialize() throws StorageException {
        List<String> appliedMigrations;
        if (!this.file.exists()) {
            appliedMigrations = new ArrayList<>();
        } else {
            if (!this.file.canRead()) {
                throw new StorageException("Database file '" + this.file + "' exists but doesn't allow reading.");
            }

            appliedMigrations = loadAppliedMigrations();
            log.fine(() -> appliedMigrations.size() + " migration(s) already applied.");
        }

        Map<String, String> migrations = readMigrations();

        log.fine("Found " + migrations.size() + " migration(s) to run.");

        for (Map.Entry<String, String> entry : migrations.entrySet()) {
            String name = entry.getKey();
            String migration = entry.getValue();

            if (appliedMigrations.contains(name)) {
                log.fine(() -> "Skipping migration " + name + ", already applied.");
                continue;
            }

            log.fine(() -> "Running migration " + name);

            try (Closer closer = Closer.empty()) {
                Connection connection = closer.register(openConnection());

                PreparedStatement statement = closer.register(connection.prepareStatement(migration));
                statement.execute();

                PreparedStatement insertStatement = closer.register(connection.prepareStatement(
                        "INSERT INTO `migrations` (`name`) VALUES (?);"
                ));

                insertStatement.setString(1, name);
                insertStatement.execute();
            } catch (IOException | SQLException e) {
                throw new StorageException(e);
            }
        }
    }

    @NotNull
    private List<String> loadAppliedMigrations() throws StorageException {
        List<String> migrations = new ArrayList<>();

        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            PreparedStatement statement = closer.register(connection.prepareStatement("SELECT `name` FROM `migrations`;"));

            ResultSet resultSet = closer.register(statement.executeQuery());

            while (resultSet.next()) {
                migrations.add(resultSet.getString(1));
            }
        } catch (IOException | SQLException e) {
            throw new StorageException(e);
        }

        return migrations;
    }

    /**
     * No retries are attempted.
     *
     * @throws RecoverableStorageException If it failed to open a connection.
     */
    @NotNull
    private Connection openConnection() {
        Future<Connection> future = EXECUTOR.submit(() -> {
            try {
                return DriverManager.getConnection(getConnectionString());
            } catch (SQLException e) {
                throw new RecoverableStorageException(e);
            }
        });

        try {
            return future.get(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RecoverableStorageException(e);
        }
    }

    @NotNull
    private String getConnectionString() {
        return "jdbc:sqlite:" + file;
    }

    private <T> Future<T> queryAsync(Callable<T> callable) {
        return EXECUTOR.submit(callable);
    }

    /**
     * @throws RecoverableStorageException on inner IOException, SQLException
     *
     */
    @Override
    public @NotNull Future<List<Region>> loadRegions() {
        return queryAsync(() ->
                RetryService.run(() -> {
                    List<Region> regions = new ArrayList<>();

                    try (Closer closer = Closer.empty()) {
                        Connection connection = closer.register(openConnection());

                        PreparedStatement statement = closer.register(connection.prepareStatement(
                                "SELECT `name`, `priority`, `all`, `disable_other_break`, " +
                                        "`type`, `cuboid_top_left`, `cuboid_bottom_right`, `world_name` FROM `regions`" +
                                        " ORDER BY `priority` DESC;"));

                        ResultSet resultSet = statement.executeQuery();

                        while (resultSet.next()) {
                            Region region = Regions.fromResultSet(resultSet);
                            regions.add(region);
                        }
                    } catch (SQLException | IOException e) {
                        throw new RecoverableStorageException(e);
                    }
                    return regions;
                })
        );
    }

    private Integer optionalBoolean(Boolean bool) {
        return bool == null ? null : (bool ? 1 : 0);
    }

    private boolean checkRegionExists() throws StorageException {
        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            PreparedStatement statement = closer.register(connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM `regions` WHERE `name`=?);"));

            ResultSet resultSet = closer.register(statement.executeQuery());

            return resultSet.getBoolean(1);
        } catch (IOException | SQLException e) {
            throw new StorageException(e);
        }
    }

    private void populateUpsert(PreparedStatement statement, Region region) throws SQLException, IllegalArgumentException {
        statement.setInt(1, region.getPriority());
        statement.setBoolean(2, region.isAll());
        statement.setObject(3, optionalBoolean(region.getDisableOtherBreak()), Types.INTEGER);

        RegionType type = Regions.getRegionType(region);
        statement.setInt(4, type.ordinal());

        switch (type) {
            case CUBOID:
                CuboidRegion cuboidRegion = (CuboidRegion) region;
                statement.setString(5, cuboidRegion.getMin().serialize());
                statement.setString(6, cuboidRegion.getMax().serialize());

                statement.setString(7, cuboidRegion.getMin().getWorldName());
                break;
            case WORLD:
                statement.setString(5, null);
                statement.setString(6, null);

                WorldRegion worldRegion = (WorldRegion) region;

                statement.setString(7, worldRegion.getName());
                break;
            default:
                throw new IllegalArgumentException("Invalid region type.");
        }

        statement.setString(8, region.getName());
    }

    @Override
    public Future<Void> saveRegion(@NotNull Region region) {
        return queryAsync(() -> {
            try (Closer closer = Closer.empty()) {
                Connection connection = closer.register(openConnection());

                connection.setAutoCommit(false);

                String toExecute;

                if (checkRegionExists()) {
                    toExecute = "UPDATE `regions`" +
                            " SET `priority`=?, `all`=?, `disable_other_break`=?," +
                            " `type`=?, `cuboid_top_left`=?, `cuboid_bottom_right`=?, `world_name`=?" +
                            " WHERE `name`=?;";
                } else {
                    toExecute = "INSERT INTO `regions`" +
                            " (`priority`, `all`, `disable_other_break`, `type`, `cuboid_top_left`, `cuboid_bottom_right`, `world_name`, `name`)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
                }

                PreparedStatement statement = closer.register(connection.prepareStatement(toExecute));

                populateUpsert(statement, region);

                statement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                throw new StorageException("Failed to insert/update region.", e);
            }
            return null;
        });
    }

    @Override
    public Future<Void> updateRegions(@NotNull Collection<Region> regions) {
        // todo: find regions that are not yet inserted in the database, should be all of them though

        return queryAsync(() -> {
            try (Closer closer = Closer.empty()) {
                Connection connection = closer.register(openConnection());

                connection.setAutoCommit(false);

                for (Region region : regions) {
                    PreparedStatement statement = closer.register(connection.prepareStatement("UPDATE `regions`" +
                            " SET `priority`=?, `all`=?, `disable_other_break`=?," +
                            " `type`=?, `cuboid_top_left`=?, `cuboid_bottom_right`=?, `world_name`=?" +
                            " WHERE `name`=?;"));

                    populateUpsert(statement, region);

                    statement.executeUpdate();
                }

                log.fine(() -> "Bulk updated all the regions.");
                connection.commit();
            }

            return null;
        });
    }

    @Override
    public Future<Void> deleteRegion(@NotNull Region region) {
        return queryAsync(() -> {
            try (Closer closer = Closer.empty()) {
                Connection connection = closer.register(openConnection());

                connection.setAutoCommit(false);

                PreparedStatement statement = closer.register(connection.prepareStatement("DELETE FROM `regions` WHERE `name`=?;"));

                statement.setString(1, region.getName());

                statement.execute();

                connection.commit();

                return null;
            }
        });
    }
}
