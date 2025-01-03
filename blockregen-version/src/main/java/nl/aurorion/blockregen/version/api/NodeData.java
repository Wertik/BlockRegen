package nl.aurorion.blockregen.version.api;

import org.bukkit.block.Block;

/* 
    Interface for NodeData implementations on different versions. NodeData holds information about the Block that's been broken. Allows to place it back with correct BlockData.
    Any implementation has to be easily serializable into Json.
*/
public interface NodeData {
    // Load the blocks block data.
    void load(Block block);

    // Place the block with corresponding Block data.
    void apply(Block block);

    // Check NodeData against a placed block.
    boolean matches(Block block);

    boolean isEmpty();

    String getPrettyString();
}
