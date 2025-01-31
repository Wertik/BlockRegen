package nl.aurorion.blockregen.material;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.util.Blocks;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public class MinecraftMaterial implements BlockRegenMaterial {

    private final BlockRegenPlugin plugin;

    @Getter
    private final XMaterial material;

    @Getter
    @Nullable
    private final NodeData nodeData;

    public MinecraftMaterial(BlockRegenPlugin plugin, XMaterial material, @Nullable NodeData nodeData) {
        this.plugin = plugin;
        this.material = material;
        this.nodeData = nodeData;
    }

    public MinecraftMaterial(BlockRegenPlugin plugin, XMaterial material) {
        this.plugin = plugin;
        this.material = material;
        this.nodeData = null;
    }

    @Override
    public boolean check(Block block) {
        boolean res = this.plugin.getVersionManager().getMethods().compareType(block, this.material);

        if (this.nodeData != null) {
            res &= this.nodeData.matches(block);
        }

        return res;
    }

    @Override
    public void applyData(Block block) {
        if (this.nodeData != null) {
            this.nodeData.apply(block);
        }
    }

    @Override
    public void setType(Block block) {
        plugin.getVersionManager().getMethods().setType(block, this.material);
    }

    @Override
    public boolean requiresSolidGround() {
        return Blocks.isMultiblockCrop(material) || Blocks.requiresFarmland(material) || Blocks.reliesOnBlockBelow(material);
    }

    @Override
    public boolean requiresFarmland() {
        return Blocks.requiresFarmland(this.material);
    }

    @Override
    public String toString() {
        return "MinecraftMaterial{" +
                "material=" + material +
                ", data=" + nodeData +
                '}';
    }
}
