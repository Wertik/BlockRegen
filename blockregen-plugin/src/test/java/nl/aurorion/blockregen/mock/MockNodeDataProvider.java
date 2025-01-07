package nl.aurorion.blockregen.mock;

import nl.aurorion.blockregen.api.version.VersionManager;
import nl.aurorion.blockregen.version.api.NodeData;

public class MockNodeDataProvider implements VersionManager.NodeDataProvider {
    @Override
    public NodeData provide() {
        return new MockNodeData();
    }
}
