package nl.aurorion.blockregen;

import lombok.Getter;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialManager;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import nl.aurorion.blockregen.preset.material.TargetMaterial;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class MaterialParsingTests {

    private final MaterialManager materialManager = new MaterialManager();

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

    public MaterialParsingTests() {
        materialManager.registerParser(null, new MaterialParser() {
            /**
             * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input = 'caveblock').
             * @throws IllegalArgumentException If the parsing fails.
             */
            @Override
            public @NotNull BlockRegenMaterial parseMaterial(String input) {
                return new MockMaterial(input);
            }
        });
    }

    @Test
    public void parsesSingleTargetMaterial() {
        BlockRegenMaterial material = materialManager.parseMaterial("random");
        assertInstanceOf(MockMaterial.class, material);
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
}
