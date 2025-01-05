package nl.aurorion.blockregen.material;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;
import org.bukkit.block.Block;

import java.util.Objects;

// Check using the Oraxen API whether the destroyed block matches.
public class OraxenMaterial implements BlockRegenMaterial {

    private final BlockRegen plugin;

    @Getter
    private final String oraxenId;

    public OraxenMaterial(BlockRegen plugin, String oraxenId) {
        this.plugin = plugin;
        this.oraxenId = oraxenId;
    }

    @Override
    public boolean check(Block block) {
        Mechanic oraxenBlock = OraxenBlocks.getOraxenBlock(block.getLocation());

        if (oraxenBlock == null) {
            return false;
        }

        String blockId = oraxenBlock.getItemID();
        return Objects.equals(blockId, this.oraxenId);
    }

    @Override
    public void setType(Block block) {
        OraxenBlocks.place(this.oraxenId, block.getLocation());
    }

    @Override
    public String toString() {
        return "OraxenMaterial{" +
                "oraxenId='" + oraxenId + '\'' +
                '}';
    }
}
