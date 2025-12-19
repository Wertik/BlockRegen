package nl.aurorion.blockregen.storage.sqlite;

import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.region.CuboidRegion;
import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import nl.aurorion.blockregen.storage.DriverProvider;
import nl.aurorion.blockregen.storage.RegionType;
import nl.aurorion.blockregen.storage.StorageDriver;
import nl.aurorion.blockregen.storage.exception.ConnectionException;
import nl.aurorion.blockregen.storage.exception.InvalidDataException;
import nl.aurorion.blockregen.storage.exception.StorageException;
import nl.aurorion.blockregen.util.BlockPosition;
import nl.aurorion.blockregen.util.Closer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

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
        public @NotNull StorageDriver create(@Nullable ConfigurationSection section) throws StorageException {
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

    private static Map<String, String> readMigrations() throws StorageException {
        Map<String, String> migrations = new HashMap<>();

        Files.readResources("sqlite/migrations/", (path) -> {
            if (!path.toString().endsWith(".sql")) {
                return;
            }

            List<String> lines;
            try {
                lines = java.nio.file.Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // use filename without suffix as a name
            String fileName = path.getFileName().toString();
            migrations.put(fileName.substring(0, fileName.length() - 4), String.join("\n", lines));
        });

        return migrations;
    }

    @Contract("null->null;!null->_")
    public static Boolean objToOptionalBoolean(@Nullable Object o) throws InvalidDataException {
        if (o == null) {
            return null;
        }

        if (o instanceof Integer) {
            return (int) o == 1;
        }

        throw new InvalidDataException("Invalid type for optional boolean " + o.getClass().getSimpleName());
    }

    @Contract("null->null;!null->_")
    public static Integer optionalBooleanToInt(Boolean bool) {
        return bool == null ? null : (bool ? 1 : 0);
    }

    @NotNull
    public static Region fromResultSet(@NotNull ResultSet resultSet) throws SQLException, InvalidDataException {
        String name = InvalidDataException.throwIfNull(resultSet.getString("name"), "'name' cannot be null.");
        int priority = resultSet.getInt("priority");
        boolean all = resultSet.getBoolean("all");
        Boolean disableOtherBreak = objToOptionalBoolean(resultSet.getObject("disable_other_break"));

        String worldName = InvalidDataException.throwIfNull(resultSet.getString("world_name"), "'world_name' cannot be null.");

        RegionType type;
        try {
            type = RegionType.of(resultSet.getInt("type"));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e);
        }

        Region region;
        switch (type) {
            case CUBOID:
                String minString = InvalidDataException.throwIfNull(
                        resultSet.getString("cuboid_min"),
                        "'cuboid_min' cannot be null.");
                String maxString = InvalidDataException.throwIfNull(
                        resultSet.getString("cuboid_max"),
                        "'cuboid_max' cannot be null.'");

                BlockPosition min = BlockPosition.from(worldName, minString);
                BlockPosition max = BlockPosition.from(worldName, maxString);

                region = CuboidRegion.create(name, min, max);
                break;
            case WORLD:
                region = WorldRegion.create(name, worldName);
                break;
            default:
                throw new InvalidDataException("Unsupported region type " + type.name());
        }

        region.setPriority(priority);
        region.setAll(all);
        region.setDisableOtherBreak(disableOtherBreak);

        return region;
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

            try {
                appliedMigrations = loadAppliedMigrations();
            } catch (ConnectionException | SQLException | IOException e) {
                throw new StorageException("Failed to load previously applied migrations.", e);
            }
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
            } catch (ConnectionException | IOException | SQLException e) {
                throw new StorageException("Failed to run migrations.", e);
            }
        }
    }

    @Override
    public @NotNull List<Region> loadRegions() throws StorageException {
        List<Region> regions = new ArrayList<>();

        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            PreparedStatement statement = closer.register(connection.prepareStatement(
                    "SELECT `name`, `priority`, `all`, `disable_other_break`, " +
                            "`type`, `cuboid_top_left`, `cuboid_bottom_right`, `world_name` FROM `regions`" +
                            " ORDER BY `priority` DESC;"));

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Region region = fromResultSet(resultSet);
                regions.add(region);
            }
        } catch (ConnectionException | SQLException | IOException | InvalidDataException e) {
            throw new StorageException(e);
        }
        return regions;
    }

    @Override
    public void saveRegion(@NotNull Region region) throws StorageException {
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
        } catch (ConnectionException | SQLException | IOException e) {
            throw new StorageException("Failed to insert/update region.", e);
        }
    }

    @Override
    public void updateRegions(@NotNull Collection<Region> regions) throws StorageException {
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
        } catch (ConnectionException | SQLException | IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void deleteRegion(@NotNull Region region) throws StorageException {
        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            connection.setAutoCommit(false);

            PreparedStatement statement = closer.register(connection.prepareStatement("DELETE FROM `regions` WHERE `name`=?;"));

            statement.setString(1, region.getName());

            statement.execute();

            connection.commit();
        } catch (ConnectionException | SQLException | IOException e) {
            throw new StorageException(e);
        }
    }

    @NotNull
    private List<String> loadAppliedMigrations() throws SQLException, IOException, ConnectionException {
        List<String> migrations = new ArrayList<>();

        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            PreparedStatement statement = closer.register(connection.prepareStatement("SELECT `name` FROM `migrations`;"));

            ResultSet resultSet = closer.register(statement.executeQuery());

            while (resultSet.next()) {
                migrations.add(resultSet.getString(1));
            }
        }

        return migrations;
    }

    @NotNull
    private Connection openConnection() throws ConnectionException {
        Future<Connection> future = EXECUTOR.submit(() -> {
            try {
                return DriverManager.getConnection(getConnectionString());
            } catch (SQLException e) {
                throw new ConnectionException(e);
            }
        });

        try {
            return future.get(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ConnectionException(e);
        }
    }

    @NotNull
    private String getConnectionString() {
        return "jdbc:sqlite:" + file;
    }

    private boolean checkRegionExists() throws SQLException, IOException, ConnectionException {
        try (Closer closer = Closer.empty()) {
            Connection connection = closer.register(openConnection());

            PreparedStatement statement = closer.register(connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM `regions` WHERE `name`=?);"));

            ResultSet resultSet = closer.register(statement.executeQuery());

            return resultSet.getBoolean(1);
        }
    }

    private void populateUpsert(PreparedStatement statement, Region region) throws SQLException, IllegalArgumentException {
        statement.setInt(1, region.getPriority());
        statement.setBoolean(2, region.isAll());
        statement.setObject(3, optionalBooleanToInt(region.getDisableOtherBreak()), Types.INTEGER);

        RegionType type = RegionType.of(region);
        statement.setInt(4, type.ordinal());

        switch (type) {
            case CUBOID:
                CuboidRegion cuboidRegion = (CuboidRegion) region;
                statement.setString(5, cuboidRegion.getMin().serializeCoords());
                statement.setString(6, cuboidRegion.getMax().serializeCoords());

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
}
