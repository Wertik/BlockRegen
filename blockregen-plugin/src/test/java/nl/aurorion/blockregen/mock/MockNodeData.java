package nl.aurorion.blockregen.mock;

import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.block.Block;

public class MockNodeData implements NodeData {
    @Override
    public void load(Block block) {
        //
    }

    @Override
    public void apply(Block block) {
        //
    }

    @Override
    public boolean matches(Block block) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String getPrettyString() {
        return "mock";
    }
}
