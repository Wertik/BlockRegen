package nl.aurorion.blockregen.system.preset.struct.material;

import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;

public interface TargetMaterial {

    /**
     * Return true if the block matches this material, false otherwise.
     */
    boolean check(BlockPreset preset, Block block);

    /**
     * Change the block to this material.
     */
    void place(Block block);

    default void applyData(Block block) {
        //
    }
}
