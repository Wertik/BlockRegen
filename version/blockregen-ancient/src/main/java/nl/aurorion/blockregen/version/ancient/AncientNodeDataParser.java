package nl.aurorion.blockregen.version.ancient;

import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;

public class AncientNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<AncientNodeData> nodeDataDeserializer = new NodeDataDeserializer<AncientNodeData>()
            .property("age", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, CropState.class))
            .property("facing", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, BlockFace.class))
            .property("species", (nodeData, value) -> NodeDataDeserializer.tryParseEnum(value, TreeSpecies.class))
            .property("inverted", (nodeData, value) -> nodeData.setInverted(Boolean.parseBoolean(value)))
            .property("skull", AncientNodeData::setSkull);

    @Override
    public NodeData parse(String input) {
        AncientNodeData nodeData = new AncientNodeData();
        this.nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
