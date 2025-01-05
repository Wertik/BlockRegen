package nl.aurorion.blockregen.preset.material;

import nl.aurorion.blockregen.material.BlockRegenMaterial;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// A collection of BlockRegen materials to match against.
public class TargetMaterial {
    private final List<BlockRegenMaterial> materials;

    private TargetMaterial(Collection<BlockRegenMaterial> materials) {
        this.materials = new ArrayList<>(materials);
    }

    private TargetMaterial(BlockRegenMaterial material) {
        this.materials = new ArrayList<>();
        this.materials.add(material);
    }

    @NotNull
    public static TargetMaterial of(@NotNull BlockRegenMaterial material) {
        return new TargetMaterial(material);
    }

    @NotNull
    public static TargetMaterial of(@NotNull Collection<BlockRegenMaterial> materials) {
        return new TargetMaterial(materials);
    }

    // Returns whether any of the target materials match.
    public boolean matches(@NotNull Block block) {
        for (BlockRegenMaterial targetMaterial : this.materials) {
            if (targetMaterial.check(block)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "TargetMaterial{" +
                "materials=" + materials +
                '}';
    }
}
