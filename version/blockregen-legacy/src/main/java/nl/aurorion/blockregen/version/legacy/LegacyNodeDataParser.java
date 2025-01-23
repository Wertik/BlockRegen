package nl.aurorion.blockregen.version.legacy;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;

@Log
public class LegacyNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<LegacyNodeData> nodeDataDeserializer = new NodeDataDeserializer<LegacyNodeData>()
            .property("age", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, CropState.class))
            .property("facing", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, BlockFace.class))
            .property("species", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, TreeSpecies.class))
            .property("inverted", (nodeData, value) -> nodeData.setInverted(Boolean.parseBoolean(value)))
            .property("skull", LegacyNodeData::setSkull);

    @Override
    public NodeData parse(String input) {
        LegacyNodeData nodeData = new LegacyNodeData();
        this.nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
