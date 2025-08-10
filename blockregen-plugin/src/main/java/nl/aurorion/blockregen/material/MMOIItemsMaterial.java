package nl.aurorion.blockregen.material;

import lombok.extern.java.Log;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import net.Indyuce.mmoitems.api.util.MushroomState;
import nl.aurorion.blockregen.BlockRegenPlugin;
import org.bukkit.block.Block;

import java.util.Optional;

@Log
public class MMOIItemsMaterial implements BlockRegenMaterial {

    private final BlockRegenPlugin plugin;

    private final int id;

    public MMOIItemsMaterial(BlockRegenPlugin plugin, int id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public boolean check(Block block) {
        Optional<CustomBlock> customBlock = MMOItems.plugin.getCustomBlocks().getFromBlock(block.getBlockData());
        return customBlock.isPresent();
    }

    @Override
    public void setType(Block block) {
        MushroomState customState = MMOItems.plugin.getCustomBlocks().getBlock(id).getState();

        block.setType(customState.getType(), false);
        block.setBlockData(customState.getBlockData(), false);
    }

    @Override
    public String toString() {
        return "MMOIItemsMaterial{" +
                "id=" + id +
                '}';
    }
}
