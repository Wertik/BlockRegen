package nl.aurorion.blockregen.system.preset.struct.material;

import java.util.*;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import nl.aurorion.blockregen.util.DiscreteGenerator;
import org.jetbrains.annotations.NotNull;

// A collection of materials with weighted chances of being picked.
@Log
public class PlacementMaterial {

    private final DiscreteGenerator<BlockRegenMaterial> generator;

    private final Map<BlockRegenMaterial, Double> valuedMaterials;

    private PlacementMaterial(Map<BlockRegenMaterial, Double> valuedMaterials) {
        this.valuedMaterials = valuedMaterials;
        this.generator = DiscreteGenerator.fromProbabilityFunction(valuedMaterials);
    }

    @NotNull
    public static PlacementMaterial withOnlyDefault(BlockRegenMaterial defaultMaterial) {
        Map<BlockRegenMaterial, Double> valued = new HashMap<>();
        valued.put(defaultMaterial, 1.0);
        return new PlacementMaterial(valued);
    }

    @NotNull
    public static PlacementMaterial from(Map<BlockRegenMaterial, Double> valuedMaterials) {
        return new PlacementMaterial(valuedMaterials);
    }

    public Map<BlockRegenMaterial, Double> getValuedMaterials() {
        return Collections.unmodifiableMap(valuedMaterials);
    }

    @NotNull
    public BlockRegenMaterial get() {
        return this.generator.next();
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<BlockRegenMaterial, Double> entry : valuedMaterials.entrySet()) {
            builder.append(String.format("%s: %.2f", entry.getKey(), entry.getValue())).append(",");
        }
        return "DynamicMaterial{" +
                "valuedMaterials=" + builder.substring(0, builder.length() - 1) +
                '}';
    }
}
