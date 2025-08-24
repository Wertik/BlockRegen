package nl.aurorion.blockregen;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialManager;
import nl.aurorion.blockregen.material.MinecraftMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import nl.aurorion.blockregen.mock.MockBlockRegenPlugin;
import nl.aurorion.blockregen.preset.material.PlacementMaterial;
import nl.aurorion.blockregen.preset.material.TargetMaterial;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

// todo: 100% code coverage
public class MaterialParsingTests {

    private final BlockRegenPlugin plugin = new MockBlockRegenPlugin();

    private final MaterialManager materialManager = new MaterialManager(plugin);

    static class MockMaterial implements BlockRegenMaterial {

        @Getter
        private final String id;

        public MockMaterial(String id) {
            this.id = id;
        }

        @Override
        public boolean check(Block block) {
            return false;
        }

        @Override
        public void setType(Block block) {

        }
    }

    static class MockMaterialParser implements MaterialParser {

        @Getter
        @Setter
        private boolean containsColon;

        public MockMaterialParser(boolean containsColon) {
            this.containsColon = containsColon;
        }

        /**
         * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input =
         *              'caveblock').
         * @throws ParseException If the parsing fails.
         */
        @Override
        public @NotNull BlockRegenMaterial parseMaterial(String input) {
            return new MockMaterial(input);
        }

        @Override
        public boolean containsColon() {
            return this.containsColon;
        }
    }

    public MaterialParsingTests() {
        MaterialParser mockParser = new MockMaterialParser(false);
        materialManager.registerParser(null, mockParser);
        materialManager.registerParser("mock", mockParser);
    }

    @Test
    public void parsesBlockRegenMaterial() {
        BlockRegenMaterial material = materialManager.parseMaterial("random");

        assertInstanceOf(MockMaterial.class, material);
        assertEquals(((MockMaterial) material).id, "random");
    }

    @Test
    public void parsesBlockRegenMaterialWithPrefix() {
        BlockRegenMaterial material = materialManager.parseMaterial("mock:random");

        assertInstanceOf(MockMaterial.class, material);
        assertEquals(((MockMaterial) material).id, "random");
    }

    @Test
    public void parsesNumericalIds() {
        BlockRegenMaterial material = materialManager.parseMaterial("mock:42");

        assertInstanceOf(MockMaterial.class, material);
        assertEquals(((MockMaterial) material).id, "42");
    }

    @Test
    public void throwsOnInvalidPrefix() {
        assertThrows(ParseException.class, () -> materialManager.parseMaterial("invalid:random"));
    }

    @Test
    public void throwsOnNoValidParser() {
        assertThrows(ParseException.class, () -> new MaterialManager(plugin).parseMaterial("random"));
    }

    @Test
    public void throwsOnEmptyMaterial() {
        assertThrows(ParseException.class, () -> materialManager.parseMaterial(""));
    }

    @Test
    public void throwsOnInvalidTargetMaterial() {
        assertThrows(ParseException.class, () -> materialManager.parseTargetMaterial(";"));
    }

    @Test
    public void parsesMultipleTargetMaterials() {
        TargetMaterial targetMaterial = materialManager.parseTargetMaterial("first;second");

        assertEquals(2, targetMaterial.getMaterials().size());

        BlockRegenMaterial first = targetMaterial.getMaterials().get(0);
        assertInstanceOf(MockMaterial.class, first);
        BlockRegenMaterial second = targetMaterial.getMaterials().get(1);
        assertInstanceOf(MockMaterial.class, second);

        assertEquals("first", ((MockMaterial) first).getId());
        assertEquals("second", ((MockMaterial) second).getId());
    }

    @Test
    public void parsesSinglePlacementMaterial() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("first");

        assertInstanceOf(MockMaterial.class, placementMaterial.get());
        MockMaterial mockMaterial = (MockMaterial) placementMaterial.get();
        assertEquals("first", mockMaterial.getId());
    }

    @Test
    public void parsesSinglePlacementMaterialWithPrefix() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("mock:first");

        assertInstanceOf(MockMaterial.class, placementMaterial.get());
        MockMaterial mockMaterial = (MockMaterial) placementMaterial.get();
        assertEquals("first", mockMaterial.getId());
    }

    @Test
    public void parsesSinglePlacementMaterialWithPrefixAndChance() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("mock:first:10");

        assertEquals(2, placementMaterial.getValuedMaterials().size());

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("first"),
                0.1);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MinecraftMaterial && ((MinecraftMaterial) m).getMaterial() == XMaterial.AIR,
                0.9);
    }

    @NotNull
    private static Map.Entry<BlockRegenMaterial, Double> assertPlacementMaterial(PlacementMaterial placementMaterial, Predicate<BlockRegenMaterial> which, double chance) {
        Map.Entry<BlockRegenMaterial, Double> material = placementMaterial.getValuedMaterials().entrySet().stream()
                .filter(e -> which.test(e.getKey()))
                .findAny()
                .orElse(null);
        assertNotNull(material);
        assertEquals(chance, material.getValue(), 0.01);
        return material;
    }

    @Test
    public void parsesMultiplePlacementMaterials() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("first:10;second:20");

        assertEquals(3, placementMaterial.getValuedMaterials().size());

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("first"),
                0.1);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("second"),
                0.2);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MinecraftMaterial && ((MinecraftMaterial) m).getMaterial() == XMaterial.AIR,
                0.7);
    }

    @Test
    public void restMaterialsDistributeEvenly() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("first;second");

        assertEquals(2, placementMaterial.getValuedMaterials().size());

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("first"),
                0.5);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("second"),
                0.5);
    }

    @Test
    public void placementMaterialAddsUpToOne() {
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("first:50;second:50");

        assertEquals(2, placementMaterial.getValuedMaterials().size());

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("first"),
                0.5);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("second"),
                0.5);
    }

    @Test
    public void parsesMaterialsWithColon() {
        // Expect to return namespace:first for the material id.
        ((MockMaterialParser) Objects.requireNonNull(materialManager.getParser("mock"))).setContainsColon(true);
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("mock:namespace:first;mock:namespace:second");
        ((MockMaterialParser) Objects.requireNonNull(materialManager.getParser("mock"))).setContainsColon(false);

        assertEquals(2, placementMaterial.getValuedMaterials().size());

        assertTrue(placementMaterial.getValuedMaterials().keySet().stream()
                .anyMatch(m -> ((MockMaterial) m).getId().equals("namespace:first")));
        assertTrue(placementMaterial.getValuedMaterials().keySet().stream()
                .anyMatch(m -> ((MockMaterial) m).getId().equals("namespace:second")));

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("namespace:first"),
                0.5);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("namespace:second"),
                0.5);
    }

    @Test
    public void parsesMaterialsWithColonAndChance() {
        // Expect to return namespace:first for the material id.
        ((MockMaterialParser) Objects.requireNonNull(materialManager.getParser("mock"))).setContainsColon(true);
        PlacementMaterial placementMaterial = materialManager.parsePlacementMaterial("mock:namespace:first:40;mock:namespace:second:60");
        ((MockMaterialParser) Objects.requireNonNull(materialManager.getParser("mock"))).setContainsColon(false);

        assertEquals(2, placementMaterial.getValuedMaterials().size());

        assertTrue(placementMaterial.getValuedMaterials().keySet().stream()
                .anyMatch(m -> ((MockMaterial) m).getId().equals("namespace:first")));
        assertTrue(placementMaterial.getValuedMaterials().keySet().stream()
                .anyMatch(m -> ((MockMaterial) m).getId().equals("namespace:second")));

        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("namespace:first"),
                0.4);
        assertPlacementMaterial(placementMaterial,
                m -> m instanceof MockMaterial && ((MockMaterial) m).getId().equals("namespace:second"),
                0.6);
    }
}
