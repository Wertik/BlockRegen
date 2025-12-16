package nl.aurorion.blockregen;

import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import nl.aurorion.blockregen.storage.RetryService;
import nl.aurorion.blockregen.storage.StorageDriver;
import nl.aurorion.blockregen.storage.StorageException;
import nl.aurorion.blockregen.storage.sqlite.SQLiteStorageDriver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegionStorageTests {

    private final static ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    @BeforeEach
    public void before() {
        new File("./blockregen-test.db").delete();
    }

    @Test
    public void startsStorage() {
        String input = "Path: \"blockregen-test.db\"";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));

        StorageDriver driver = (SQLiteStorageDriver.Provider.in(
                        new File("./"))
                .create(conf));

        try {
            driver.initialize();
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }

        Region region = WorldRegion.create("random", "world");

        try {
            driver.saveRegion(region).get();

            List<Region> regions = driver.loadRegions().get();

            assertEquals(1, regions.size());
        } catch (InterruptedException | ExecutionException | StorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void retryService() {
        AtomicBoolean fail = new AtomicBoolean(false);

        Supplier<Integer> supplier = () -> {
            Future<Integer> future = EXECUTOR.submit(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (fail.get()) {
                    throw new RuntimeException("Mocked a fail.");
                }

                return 42;
            });

            try {
                return future.get(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        };

        int r = RetryService.run(supplier, 3);
        assertEquals(42, r);

        fail.set(true);

        assertThrows(RuntimeException.class, () -> RetryService.run(supplier, 3));
    }
}
