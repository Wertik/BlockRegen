package nl.aurorion.blockregen.mock;

import nl.aurorion.blockregen.version.VersionManager;
import nl.aurorion.blockregen.version.api.*;

public class MockVersionManager implements VersionManager {
    @Override
    public void load() {

    }

    @Override
    public NodeData createNodeData() {
        return null;
    }

    @Override
    public String loadNMSVersion() {
        return "";
    }

    @Override
    public boolean isCurrentAbove(String versionString, boolean include) {
        return false;
    }

    @Override
    public boolean isCurrentBelow(String versionString, boolean include) {
        return false;
    }

    @Override
    public boolean useCustomModelData() {
        return false;
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public WorldEditProvider getWorldEditProvider() {
        return null;
    }

    @Override
    public WorldGuardProvider getWorldGuardProvider() {
        return null;
    }

    @Override
    public Methods getMethods() {
        return new MockMethods();
    }

    @Override
    public NodeDataProvider getNodeProvider() {
        return new MockNodeDataProvider();
    }

    @Override
    public NodeDataParser getNodeDataParser() {
        return null;
    }

    @Override
    public void registerVersionedListeners() {
        //
    }
}
