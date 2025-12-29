package nl.aurorion.blockregen;

import nl.aurorion.blockregen.util.Versions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionsTests {

    @Test
    public void comparesVersionsOfSameLengths() {
        assertEquals(1, Versions.compareVersions("1.10", "1.9"));
        assertEquals(-1, Versions.compareVersions("1.9", "1.10"));
        assertEquals(0, Versions.compareVersions("1.9", "1.9"));

        assertEquals(1, Versions.compareVersions("1.10.1", "1.9.2"));
        assertEquals(-1, Versions.compareVersions("1.9.12", "1.10.1"));
        assertEquals(0, Versions.compareVersions("1.9.12", "1.9.12"));
    }

    @Test
    public void comparesVersionsOfDifferentLengths() {
        // Second version longer
        assertEquals(1, Versions.compareVersions("1.10", "1.9.1"));
        assertEquals(-1, Versions.compareVersions("1.9", "1.10.2"));
        assertEquals(0, Versions.compareVersions("1.9", "1.9.0"));

        // First version longer
        assertEquals(1, Versions.compareVersions("1.10.2", "1.9"));
        assertEquals(-1, Versions.compareVersions("1.9.12", "1.10"));
        assertEquals(0, Versions.compareVersions("1.9.0", "1.9"));
    }
}
