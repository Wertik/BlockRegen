package nl.aurorion.blockregen.listener;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.region.RegionSelection;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@Log
public class PlayerListener implements Listener {

    private final BlockRegenPluginImpl plugin;

    public PlayerListener(BlockRegenPluginImpl instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Ignore offhand events at 1.9+
        if (plugin.getVersionManager().isCurrentAbove("1.9", true) && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        // Ignore other interacts.
        if ((event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null) {
            return;
        }

        // Data check

        if (plugin.getRegenerationManager().hasDataCheck(player)) {
            event.setCancelled(true);

            XMaterial material;
            try {
                material = plugin.getVersionManager().getMethods().getType(event.getClickedBlock());
            } catch (IllegalArgumentException e) {
                log.fine("Unknown block material " + event.getClickedBlock().getType() + " in data check.");
                Message.UNKNOWN_MATERIAL.send(player);
                return;
            }

            NodeData data = plugin.getVersionManager().createNodeData();
            data.load(event.getClickedBlock());

            Message.DATA_CHECK.mapAndSend(player, str -> str.replace("%block%", material.name()));
            if (!data.isEmpty()) {
                Message.DATA_CHECK_NODE_DATA.mapAndSend(player, str -> str.replace("%data%", String.format("%s%s", material.name(), data.getPrettyString())));
            }
            return;
        }

        ItemStack tool = plugin.getVersionManager().getMethods().getItemInMainHand(player);

        XMaterial toolMaterial;
        try {
            toolMaterial = XMaterial.matchXMaterial(tool);
        } catch (IllegalArgumentException e) {
            log.fine(() -> String.format("Unknown tool material %s.", tool.getType()));
            if (!BlockRegenPluginImpl.getInstance().getConfig().getBoolean("Ignore-Unknown-Materials", false)) {
                throw e;
            }
            return;
        }

        // Region selection

        // Use our own selection only if WorldEdit is not installed.
        if (player.hasPermission("blockregen.select") && plugin.getVersionManager().getWorldEditProvider() == null && toolMaterial == XMaterial.WOODEN_AXE) {
            RegionSelection selection = plugin.getRegionManager().getOrCreateSelection(player);

            // Selecting first.
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                selection.setFirst(event.getClickedBlock().getLocation());

                Message.SELECT_FIRST.mapAndSend(player, str -> str
                        .replace("%x%", String.format("%.0f", selection.getFirst().getX()))
                        .replace("%y%", String.format("%.0f", selection.getFirst().getY()))
                        .replace("%z%", String.format("%.0f", selection.getFirst().getZ())));
            } else {
                // Selecting second.
                selection.setSecond(event.getClickedBlock().getLocation());

                Message.SELECT_SECOND.mapAndSend(player, str -> str
                        .replace("%x%", String.format("%.0f", selection.getSecond().getX()))
                        .replace("%y%", String.format("%.0f", selection.getSecond().getY()))
                        .replace("%z%", String.format("%.0f", selection.getSecond().getZ())));
            }

            event.setCancelled(true);
            return;
        }

        // Adding presets to regions,... with a shovel?

        RegenerationArea region = plugin.getRegionManager().getArea(event.getClickedBlock());

        if (player.hasPermission("blockregen.region") && toolMaterial == XMaterial.WOODEN_SHOVEL && region != null) {
            event.setCancelled(true);

            BlockPreset preset = plugin.getPresetManager().getPreset(event.getClickedBlock());

            if (preset == null) {
                RegenerationProcess process = plugin.getRegenerationManager().getProcess(event.getClickedBlock());
                if (process == null) {
                    return;
                }
                preset = process.getPreset();
            }

            final String presetName = preset.getName();

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Add a block

                if (region.hasPreset(preset.getName())) {
                    Message.HAS_PRESET_ALREADY.mapAndSend(player, str -> str
                            .replace("%region%", region.getName())
                            .replace("%preset%", presetName));
                    return;
                }

                region.addPreset(presetName);

                Message.PRESET_ADDED.mapAndSend(player, str -> str
                        .replace("%region%", region.getName())
                        .replace("%preset%", presetName));
            } else {
                // Remove a block

                if (!region.hasPreset(presetName)) {
                    Message.DOES_NOT_HAVE_PRESET.mapAndSend(player, str -> str
                            .replace("%region%", region.getName())
                            .replace("%preset%", presetName));
                    return;
                }

                region.removePreset(presetName);

                Message.PRESET_REMOVED.mapAndSend(player, str -> str
                        .replace("%region%", region.getName())
                        .replace("%preset%", presetName));
            }
        }
    }

    // Inform about a new version
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // newVersion will be null when the checker is disabled, or there are no new available
        if (player.hasPermission("blockregen.admin") && plugin.newVersion != null) {
            Message.UPDATE.mapAndSend(player, str -> str
                    .replaceAll("(?i)%newVersion%", plugin.newVersion)
                    .replaceAll("(?i)%version%", plugin.getDescription().getVersion()));
        }

        // Add to bars if needed
        plugin.getEventManager().addBars(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().removeBars(event.getPlayer());
    }
}