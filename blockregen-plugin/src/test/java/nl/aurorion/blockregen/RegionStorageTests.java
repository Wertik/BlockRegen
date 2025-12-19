package nl.aurorion.blockregen;

import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.region.WorldRegion;
import nl.aurorion.blockregen.storage.StorageDriver;
import nl.aurorion.blockregen.storage.exception.StorageException;
import nl.aurorion.blockregen.storage.sqlite.SQLiteStorageDriver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionStorageTests {

    @BeforeEach
    public void before() {
        new File("./blockregen-test.db").delete();
    }

    @Test
    public void startsStorage() throws StorageException {
        String input = "Path: \"blockregen-test.db\"";
        FileConfiguration conf = YamlConfiguration.loadConfiguration(new StringReader(input));

        StorageDriver driver = (SQLiteStorageDriver.Provider.in(
                        new File("./"))
                .create(conf));

        driver.initialize();

        Region region = WorldRegion.create("random", "world");

        driver.saveRegion(region);

        List<Region> regions = driver.loadRegions();

        assertEquals(1, regions.size());
    }
}
