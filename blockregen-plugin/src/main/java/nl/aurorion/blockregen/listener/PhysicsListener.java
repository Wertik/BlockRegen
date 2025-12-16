package nl.aurorion.blockregen.listener;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.regeneration.RegenerationProcess;
import nl.aurorion.blockregen.region.Region;
import nl.aurorion.blockregen.util.Blocks;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.List;

@Log
public class PhysicsListener implements Listener {

    private final BlockRegenPlugin plugin;

    // Cache some options, when being called in physics and block break, the memory section lookup takes a long time.
    private boolean disablePhysics;
    private boolean useRegions;
    private List<String> worldsEnabled;

    public PhysicsListener(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.disablePhysics = !plugin.getConfig().isSet("Disable-Physics") || plugin.getConfig().getBoolean("Disable-Physics", false);
        this.useRegions = plugin.getConfig().getBoolean("Use-Regions");
        this.worldsEnabled = plugin.getConfig().getStringList("Worlds-Enabled");
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        if (!this.disablePhysics) {
            return;
        }

        Block block = event.getBlock();
        World world = block.getWorld();

        boolean useRegions = this.useRegions;
        Region region = plugin.getRegionManager().getRegion(block);

        boolean isInWorld = this.worldsEnabled.contains(world.getName());
        boolean isInRegion = region != null;

        boolean isInZone = useRegions ? isInRegion : isInWorld;

        if (!isInZone) {
            return;
        }

        // Only deny physics if the update is caused by a regenerating block.
        RegenerationProcess process = plugin.getRegenerationManager().getProcess(event.getSourceBlock());
        if (process == null || !process.getPreset().isDisablePhysics()) {
            return;
        }
        event.setCancelled(true);
        log.fine(() -> event.getChangedType() + " " + Blocks.blockToString(event.getBlock()));
    }
}
