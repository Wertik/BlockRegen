package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;

@NoArgsConstructor
public class PinkNodeDataParser extends LatestNodeDataParser {

    protected <T extends PinkNodeData> NodeDataDeserializer<T> createPinkDeserializer() {
        NodeDataDeserializer<T> deserializer = super.createLatestDeserializer();
        return deserializer
                .property("flowerAmount", (data, value) -> data.setFlowerAmount(Integer.parseInt(value)));
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @Override
    public NodeData parse(String input) {
        PinkNodeData nodeData = new PinkNodeData();
        createPinkDeserializer().deserialize(nodeData, input);
        return nodeData;
    }
}
