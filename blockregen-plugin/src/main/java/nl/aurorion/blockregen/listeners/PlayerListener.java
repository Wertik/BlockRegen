package nl.aurorion.blockregen.listeners;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.system.preset.BlockPreset;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.system.region.struct.RegionSelection;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

@Log
public class PlayerListener implements Listener {

    private final BlockRegen plugin;

    public PlayerListener(BlockRegen instance) {
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

        // Region selection

        // Use our own selection only if WorldEdit is not installed.

        XMaterial handMaterial = XMaterial.matchXMaterial(plugin.getVersionManager().getMethods().getItemInMainHand(player));

        if (player.hasPermission("blockregen.select") && handMaterial == XMaterial.WOODEN_AXE && plugin.getVersionManager().getWorldEditProvider() == null) {
            RegionSelection selection = plugin.getRegionManager().getOrCreateSelection(player);

            // Selecting first.
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                selection.setFirst(event.getClickedBlock().getLocation());

                player.sendMessage(Message.SELECT_FIRST.get(player)
                        .replace("%x%", String.format("%.0f", selection.getFirst().getX()))
                        .replace("%y%", String.format("%.0f", selection.getFirst().getY()))
                        .replace("%z%", String.format("%.0f", selection.getFirst().getZ())));
            } else {
                // Selecting second.
                selection.setSecond(event.getClickedBlock().getLocation());

                player.sendMessage(Message.SELECT_SECOND.get(player)
                        .replace("%x%", String.format("%.0f", selection.getSecond().getX()))
                        .replace("%y%", String.format("%.0f", selection.getSecond().getY()))
                        .replace("%z%", String.format("%.0f", selection.getSecond().getZ())));
            }

            event.setCancelled(true);
            return;
        }

        // Adding presets to regions,... with a shovel?

        RegenerationArea region = plugin.getRegionManager().getArea(event.getClickedBlock());

        if (player.hasPermission("blockregen.region") && handMaterial == XMaterial.WOODEN_SHOVEL && region != null) {
            event.setCancelled(true);

            BlockPreset preset = plugin.getPresetManager().getPreset(event.getClickedBlock());

            if (preset == null) {

                RegenerationProcess process = plugin.getRegenerationManager().getProcess(event.getClickedBlock());

                if (process == null) {
                    return;
                }

                preset = process.getPreset();
            }

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Add a block

                if (region.hasPreset(preset.getName())) {
                    player.sendMessage(Message.HAS_PRESET_ALREADY.get(player)
                            .replace("%region%", region.getName())
                            .replace("%preset%", preset.getName()));
                    return;
                }

                region.addPreset(preset.getName());

                player.sendMessage(Message.PRESET_ADDED.get(player)
                        .replace("%region%", region.getName())
                        .replace("%preset%", preset.getName()));
            } else {
                // Remove a block

                if (!region.hasPreset(preset.getName())) {
                    player.sendMessage(Message.DOES_NOT_HAVE_PRESET.get(player)
                            .replace("%region%", region.getName())
                            .replace("%preset%", preset.getName()));
                    return;
                }

                region.removePreset(preset.getName());

                player.sendMessage(Message.PRESET_REMOVED.get(player)
                        .replace("%region%", region.getName())
                        .replace("%preset%", preset.getName()));
            }

            return;
        }

        // Data check

        if (plugin.getRegenerationManager().hasDataCheck(player)) {
            event.setCancelled(true);

            XMaterial material = plugin.getVersionManager().getMethods().getType(event.getClickedBlock());

            NodeData data = plugin.getVersionManager().createNodeData();
            data.load(event.getClickedBlock());

            player.sendMessage(Message.DATA_CHECK.get(player).replace("%block%", material == null ? "Unsupported material" : material.name()));
            if (!data.isEmpty()) {
                player.sendMessage(Message.DATA_CHECK_NODE_DATA.get(player).replace("%data%", String.format("%s%s", material == null ? "Unsupported material" : material.name(), data.getPrettyString())));
            }
        }
    }

    // Inform about a new version
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // newVersion will be null when the checker is disabled, or there are no new available
        if (player.hasPermission("blockregen.admin") && plugin.newVersion != null) {
            player.sendMessage(Message.UPDATE.get(player)
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