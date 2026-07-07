package nl.aurorion.blockregen.compatibility.material;

import com.cryptomorin.xseries.XMaterial;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import lombok.Getter;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

public class CraftEngineMaterial implements BlockRegenMaterial {

    @Getter
    private final Key blockId;

    public CraftEngineMaterial(Key blockId) {
        this.blockId = blockId;
    }

    @Override
    public boolean check(Block block) {
        BukkitExistingBlock customBlock = BukkitAdaptor.adapt(block);
        if (!customBlock.isCustom()) {
            return false;
        }
        return Objects.equals(customBlock.id(), this.blockId);
    }

    @Override
    public void setType(Block block) {
        CraftEngineBlocks.place(block.getLocation(), this.blockId, true);
    }

    @Override
    public XMaterial getType() {
        BlockDefinition blockDefinition = CraftEngineBlocks.byId(this.blockId);
        if (blockDefinition == null) {
            throw new IllegalArgumentException(String.format("Invalid CraftEngine material: %s", this.blockId));
        }
        return XMaterial.matchXMaterial(CraftEngineBlocks.getBukkitBlockData(blockDefinition.defaultState()).getMaterial());
    }

    @Override
    public String getConfigurationString() {
        return this.blockId.asString();
    }

    @Override
    public String toString() {
        return "CraftEngineMaterial{" +
                "blockId='" + blockId + '\'' +
                '}';
    }
}
