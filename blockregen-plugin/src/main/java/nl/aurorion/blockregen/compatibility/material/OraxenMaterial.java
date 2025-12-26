package nl.aurorion.blockregen.compatibility.material;

import com.cryptomorin.xseries.XMaterial;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

// Check using the Oraxen API whether the destroyed block matches.
public class OraxenMaterial implements BlockRegenMaterial {

    private final transient BlockRegenPlugin plugin;

    @Getter
    private final String oraxenId;

    public OraxenMaterial(BlockRegenPlugin plugin, String oraxenId) {
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
    public XMaterial getType() {
        BlockData blockData = OraxenBlocks.getOraxenBlockData(this.oraxenId);
        if (blockData == null) {
            throw new IllegalArgumentException(String.format("Invalid oraxen material: %s", this.oraxenId));
        }
        return XMaterial.matchXMaterial(blockData.getMaterial());
    }

    @Override
    public String toString() {
        return "OraxenMaterial{" +
                "oraxenId='" + oraxenId + '\'' +
                '}';
    }
}
