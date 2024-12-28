package nl.aurorion.blockregen.system.preset.struct.material;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.util.BlockUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.block.Block;

public class MinecraftMaterial implements TargetMaterial {

    private final BlockRegen plugin;

    private final XMaterial material;

    private final NodeData nodeData;

    public MinecraftMaterial(BlockRegen plugin, XMaterial material, NodeData nodeData) {
        this.plugin = plugin;
        this.material = material;
        this.nodeData = nodeData;
    }

    public MinecraftMaterial(BlockRegen plugin, XMaterial material) {
        this.plugin = plugin;
        this.material = material;
        this.nodeData = null;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        boolean res = this.plugin.getVersionManager().getMethods().compareType(block, this.material);

        if (this.nodeData != null) {
            res &= this.nodeData.check(block);
        }

        return res;
    }

    @Override
    public void applyData(Block block) {
        if (this.nodeData != null) {
            this.nodeData.place(block);
        }
    }

    @Override
    public void place(Block block) {
        plugin.getVersionManager().getMethods().setType(block, this.material);
    }

    @Override
    public boolean requiresSolidGround() {
        return BlockUtil.isMultiblockCrop(material) || BlockUtil.requiresFarmland(material) || BlockUtil.reliesOnBlockBelow(material);
    }

    @Override
    public boolean requiresFarmland() {
        return BlockUtil.requiresFarmland(this.material);
    }

    @Override
    public String toString() {
        return "MinecraftMaterial{" +
                "material=" + material +
                ", data=" + nodeData +
                '}';
    }
}
