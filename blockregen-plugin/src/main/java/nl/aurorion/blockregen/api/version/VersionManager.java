package nl.aurorion.blockregen.api.version;

import nl.aurorion.blockregen.version.api.*;

public interface VersionManager {
    void load();

    NodeData createNodeData();

    String loadNMSVersion();

    boolean isCurrentAbove(String versionString, boolean include);

    boolean isCurrentBelow(String versionString, boolean include);

    boolean useCustomModelData();

    String getVersion();

    WorldEditProvider getWorldEditProvider();

    WorldGuardProvider getWorldGuardProvider();

    Methods getMethods();

    NodeDataProvider getNodeProvider();

    NodeDataParser getNodeDataParser();

    interface NodeDataProvider {
        NodeData provide();
    }
}
