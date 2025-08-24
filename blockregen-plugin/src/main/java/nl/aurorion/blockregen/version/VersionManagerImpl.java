package nl.aurorion.blockregen.version;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.util.Versions;
import nl.aurorion.blockregen.version.ancient.AncientMethods;
import nl.aurorion.blockregen.version.ancient.AncientNodeData;
import nl.aurorion.blockregen.version.ancient.AncientNodeDataParser;
import nl.aurorion.blockregen.version.api.*;
import nl.aurorion.blockregen.version.current.*;
import nl.aurorion.blockregen.version.legacy.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Log
public class VersionManagerImpl implements VersionManager {

    private final BlockRegenPlugin plugin;

    @Getter
    private final String version = loadNMSVersion();

    // Main plugin classes are the same across versions
    private WorldEditPlugin worldEdit;
    private WorldGuardPlugin worldGuard;

    @Getter
    private WorldEditProvider worldEditProvider;
    @Getter
    private WorldGuardProvider worldGuardProvider;
    @Getter
    private Methods methods;

    @Getter
    private NodeDataProvider nodeProvider;
    @Getter
    private NodeDataParser nodeDataParser;

    // 1.14+
    private boolean customModelData = false;

    public VersionManagerImpl(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        // todo: ideally move to CompatibilityManager as well.
        setupWorldEdit();
        setupWorldGuard();

        /*
         * Pink 1.20+
         * Latest/Modern - 1.13+
         * Legacy - 1.12 - 1.9
         * Ancient - 1.8 - 1.7
         */
        switch (version) {
            // Try to catch 1.7 into ancient. Might work on some occasions.
            case "1.7":
            case "1.8":
                if (worldEdit != null)
                    useWorldEdit(new LegacyWorldEditProvider(this.worldEdit));
                if (worldGuard != null)
                    useWorldGuard(new LegacyWorldGuardProvider(this.worldGuard));
                this.methods = new AncientMethods();
                this.nodeProvider = AncientNodeData::new;
                this.nodeDataParser = new AncientNodeDataParser();
                break;
            case "1.9":
            case "1.10":
            case "1.11":
            case "1.12":
                if (worldEdit != null)
                    useWorldEdit(new LegacyWorldEditProvider(this.worldEdit));
                if (worldGuard != null)
                    useWorldGuard(new LegacyWorldGuardProvider(this.worldGuard));
                this.methods = new LegacyMethods();
                this.nodeProvider = LegacyNodeData::new;
                this.nodeDataParser = new LegacyNodeDataParser();
                break;
            case "1.13":
                break;
            case "1.14":
                // 1.14 introduced custom model data NBTTag.
            case "1.15":
            case "1.16":
            case "1.17":
            case "1.18":
            case "1.19":
                if (worldEdit != null)
                    useWorldEdit(new LatestWorldEditProvider(this.worldEdit));
                if (worldGuard != null)
                    useWorldGuard(new LatestWorldGuardProvider(this.worldGuard));
                this.methods = new LatestMethods();
                this.nodeProvider = LatestNodeData::new;
                this.nodeDataParser = new LatestNodeDataParser();
                this.customModelData = true;
                break;
            case "1.20":
            case "1.21":
            default:
                if (worldEdit != null)
                    useWorldEdit(new LatestWorldEditProvider(this.worldEdit));
                if (worldGuard != null)
                    useWorldGuard(new LatestWorldGuardProvider(this.worldGuard));
                this.methods = new LatestMethods();
                this.nodeProvider = PinkNodeData::new;
                this.nodeDataParser = new PinkNodeDataParser();
                this.customModelData = true;
                break;
        }
    }

    @Override
    public NodeData createNodeData() {
        return this.nodeProvider.provide();
    }

    private void useWorldGuard(WorldGuardProvider provider) {
        if (worldGuardProvider == null) {
            this.worldGuardProvider = provider;
        }
    }

    private void useWorldEdit(WorldEditProvider provider) {
        if (worldEditProvider == null) {
            this.worldEditProvider = provider;
        }
    }

    @Override
    public String loadNMSVersion() {
        // ex.: 1.20.1-R0.1-SNAPSHOT
        String version = Bukkit.getServer().getBukkitVersion();

        // remove snapshot part
        version = version.substring(0, version.indexOf("-"));

        // remove patch version
        int lastDot = version.lastIndexOf(".");
        if (lastDot > 2) {
            version = version.substring(0, lastDot);
        }

        return version;
    }

    @Override
    public boolean isCurrentAbove(String versionString, boolean include) {
        int res = Versions.compareVersions(this.version, versionString, 2);
        return include ? res >= 0 : res > 0;
    }

    @Override
    public boolean isCurrentBelow(String versionString, boolean include) {
        int res = Versions.compareVersions(this.version, versionString, 2);
        return include ? res <= 0 : res < 0;
    }

    private void setupWorldEdit() {
        if (worldEditProvider != null) {
            return;
        }

        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");

        if (!(worldEditPlugin instanceof WorldEditPlugin)) {
            return;
        }

        this.worldEdit = (WorldEditPlugin) worldEditPlugin;
        log.info("WorldEdit found! &aUsing it for regions.");
    }

    private void setupWorldGuard() {
        if (worldGuardProvider != null) {
            return;
        }

        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (!(worldGuardPlugin instanceof WorldGuardPlugin)) {
            return;
        }

        this.worldGuard = (WorldGuardPlugin) worldGuardPlugin;
        log.info("WorldGuard found! &aRespecting it's protection.");
    }

    @Override
    public boolean useCustomModelData() {
        return customModelData;
    }
}
