package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.Axis;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;

@NoArgsConstructor
public class LatestNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<LatestNodeData> nodeDataDeserializer = new NodeDataDeserializer<LatestNodeData>()
            .property("age", (nodeData, value) -> nodeData.setAge(Integer.parseInt(value)))
            .property("facing", (nodeData, value) -> nodeData.setFacing(NodeDataDeserializer.tryParseEnum(value, BlockFace.class)))
            .property("rotation", (nodeData, value) -> nodeData.setRotation(NodeDataDeserializer.tryParseEnum(value, BlockFace.class)))
            .property("axis", (nodeData, value) -> nodeData.setAxis(NodeDataDeserializer.tryParseEnum(value, Axis.class)))
            .property("stairShape", (nodeData, value) -> nodeData.setStairShape(NodeDataDeserializer.tryParseEnum(value, Stairs.Shape.class)))
            .property("noteId", (nodeData, value) -> nodeData.setNoteId(Byte.parseByte(value)))
            .property("instrument", (nodeData, value) -> nodeData.setInstrument(NodeDataDeserializer.tryParseEnum(value, Instrument.class)))
            .property("octave", (nodeData, value) -> nodeData.setOctave(Integer.parseInt(value)))
            .property("sharped", (nodeData, value) -> nodeData.setSharped(Boolean.parseBoolean(value)))
            .property("tone", (nodeData, value) -> nodeData.setTone(NodeDataDeserializer.tryParseEnum(value, Note.Tone.class)))
            .property("powered", (nodeData, value) -> nodeData.setPowered(Boolean.parseBoolean(value)))
            .property("east", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.EAST);
                }
            })
            .property("north", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.NORTH);
                }
            })
            .property("south", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.SOUTH);
                }
            })
            .property("west", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.WEST);
                }
            })
            .property("up", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.UP);
                }
            })
            .property("down", (nodeData, value) -> {
                if (Boolean.parseBoolean(value)) {
                    nodeData.addFace(BlockFace.DOWN);
                }
            })
            .property("skull", LatestNodeData::setSkull);

    @Override
    public NodeData parse(String input) throws IllegalArgumentException {
        LatestNodeData nodeData = new LatestNodeData();
        nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
