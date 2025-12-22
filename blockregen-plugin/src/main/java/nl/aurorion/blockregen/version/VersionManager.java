package nl.aurorion.blockregen.version;

import nl.aurorion.blockregen.version.api.*;

public interface VersionManager {
    void load();

    NodeData createNodeData();

    boolean useCustomModelData();

    WorldEditProvider getWorldEditProvider();

    WorldGuardProvider getWorldGuardProvider();

    Methods getMethods();

    NodeDataProvider getNodeProvider();

    NodeDataParser getNodeDataParser();

    void registerVersionedListeners();

    interface NodeDataProvider {
        NodeData provide();
    }
}
