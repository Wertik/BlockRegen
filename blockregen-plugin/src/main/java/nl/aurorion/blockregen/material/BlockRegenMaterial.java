package nl.aurorion.blockregen.material;

import org.bukkit.block.Block;

// A material either vanilla or from other plugins used to compare and place blocks.
public interface BlockRegenMaterial {

    /**
     * Return true if the block matches this material, false otherwise.
     */
    boolean check(Block block);

    /**
     * Set the type of the block and apply data.
     */
    default void place(Block block) {
        setType(block);
        applyData(block);
    }

    /**
     * Set the type of the block.
     */
    void setType(Block block);

    /**
     * Apply any additional data of this material.
     */
    default void applyData(Block block) {
        //
    }

    /**
     * Whether the material requires a block underneath it.
     */
    default boolean requiresSolidGround() {
        return false;
    }

    /**
     * Whether this material requires farmland.
     */
    default boolean requiresFarmland() {
        return false;
    }

    /**
     * Whether to apply original data after calling #setType.
     */
    default boolean applyOriginalData() {
        return false;
    }
}
