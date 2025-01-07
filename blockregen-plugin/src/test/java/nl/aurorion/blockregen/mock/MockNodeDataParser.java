package nl.aurorion.blockregen.mock;

import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;

public class MockNodeDataParser implements NodeDataParser {
    @Override
    public NodeData parse(String input) {
        return new MockNodeData();
    }
}
