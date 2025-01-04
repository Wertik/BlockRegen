package nl.aurorion.blockregen.commands;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.BlockPreset;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.system.region.struct.RegenerationWorld;
import nl.aurorion.blockregen.system.region.struct.RegionSelection;
import nl.aurorion.blockregen.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor {

    private static final String HELP = "&8&m        &r &3BlockRegen &f%version% &8&m        &r"
            + "\n&3/%label% reload &8- &7Reload the plugin."
            + "\n&3/%label% debug &8- &7Turn on debug. Receive debug messages in chat."
            + "\n&3/%label% bypass &8- &7Bypass block regeneration."
            + "\n&3/%label% check &8- &7Check the correct material name to use. Just hit a block."
            + "\n&3/%label% tools &8- &7Gives you tools for regions."
            + "\n&3/%label% regions &8- &7List regions."
            + "\n&3/%label% region set <region> &8- &7Create a region from your selection."
            + "\n&3/%label% region world <region> <worldName> &8- &7Create a region for the world."
            + "\n&3/%label% region all <region> &8- &7Switch 'all presets' mode."
            + "\n&3/%label% region add <region> <preset> &8- &7Add a preset to the region."
            + "\n&3/%label% region remove <region> <preset> &8- &7Remove a preset from region."
            + "\n&3/%label% region clear <region> &8- &7Clear all presets from the region."
            + "\n&3/%label% region priority <region> <priority> &8- &7Set the priority of the region."
            + "\n&3/%label% region copy <region-from> <region-to> &8- &7Copy configured presets from one region to another."
            + "\n&3/%label% region delete <region> &8- &7Delete a region."
            + "\n&3/%label% regen (-p <preset>) (-r <region>) (-w <world>) &8- &7Regenerate presets based on argument switches."
            + "\n&3/%label% events &8- &7Event management."
            + "\n&3/%label% stats &8- &7Print statistics about currently running regeneration processes."
            + "\n&3/%label% discord &8- &7BlockRegen discord invite. Ask for support there.";

    private final BlockRegen plugin;

    public Commands(BlockRegen plugin) {
        this.plugin = plugin;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(StringUtil.color(HELP
                .replace("%version%", plugin.getDescription().getVersion())
                .replace("%label%", label)));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return false;
        }

        Player player;

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("blockregen.reload")) {
                    Message.NO_PERM.send(sender);
                    return false;
                }

                plugin.reload(sender);
                break;
            case "bypass":
                if (checkConsole(sender))
                    return false;

                player = (Player) sender;

                if (!player.hasPermission("blockregen.bypass")) {
                    Message.NO_PERM.send(player);
                    return false;
                }

                if (plugin.getRegenerationManager().switchBypass(player)) {
                    Message.BYPASS_ON.send(player);
                } else {
                    Message.BYPASS_OFF.send(player);
                }
                break;
            case "check":
                if (checkConsole(sender)) {
                    return false;
                }

                player = (Player) sender;

                if (!player.hasPermission("blockregen.check")) {
                    Message.NO_PERM.send(player);
                    return false;
                }

                if (plugin.getRegenerationManager().switchDataCheck(player)) {
                    Message.DATA_CHECK_ON.send(player);
                } else {
                    Message.DATA_CHECK_OFF.send(player);
                }
                break;
            case "tools": {
                if (checkConsole(sender)) {
                    return false;
                }

                player = (Player) sender;

                if (!player.hasPermission("blockregen.tools")) {
                    Message.NO_PERM.send(player);
                    return false;
                }

                giveTools(player);
                break;
            }
            case "regions": {

                if (checkConsole(sender)) {
                    return false;
                }

                player = (Player) sender;

                if (!player.hasPermission("blockregen.region")) {
                    Message.NO_PERM.send(player);
                    return false;
                }

                if (args.length > 1) {
                    sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                            .replace("%help%", String.format("/%s regions", label)));
                    return false;
                }

                listRegions(sender);
                break;
            }
            case "region": {

                if (checkConsole(sender))
                    return false;

                player = (Player) sender;

                if (!player.hasPermission("blockregen.region")) {
                    player.sendMessage(Message.NO_PERM.get(player));
                    return false;
                }

                if (args.length == 1) {
                    sendHelp(sender, label);
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "list": {
                        if (args.length > 2) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region list", label)));
                            return false;
                        }

                        listRegions(sender);
                        return false;
                    }
                    case "world": {
                        if (args.length > 4) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region world <name> <worldName>", label)));
                            return false;
                        } else if (args.length < 4) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region world <name> <worldName>", label)));
                            return false;
                        }

                        if (plugin.getRegionManager().exists(args[2])) {
                            Message.DUPLICATED_REGION.send(player);
                            return false;
                        }

                        for (RegenerationArea area : this.plugin.getRegionManager().getLoadedAreas()) {
                            if (area instanceof RegenerationWorld) {
                                RegenerationWorld world = (RegenerationWorld) area;
                                if (world.getWorldName().equals(args[3])) {
                                    Message.DUPLICATED_WORLD_REGION.send(player);
                                    return false;
                                }
                            }
                        }

                        RegenerationArea area = plugin.getRegionManager().createWorld(args[2], args[3]);
                        plugin.getRegionManager().addArea(area);
                        player.sendMessage(StringUtil.color(Message.REGION_FROM_WORLD.get(player)
                                .replace("%region%", args[2])
                                .replace("%world%", args[3])));
                        break;
                    }
                    case "priority": {
                        if (args.length > 4) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region priority <name> <priority>", label)));
                            return false;
                        } else if (args.length < 4) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region priority <name> <priority>", label)));
                            return false;
                        }

                        if (!plugin.getRegionManager().exists(args[2])) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        int priority;
                        try {
                            priority = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(Message.ARGUMENT_NOT_A_NUMBER.get(player)
                                    .replace("%arg%", "priority")
                                    .replace("%value%", args[3]));
                            break;
                        }

                        RegenerationArea area = plugin.getRegionManager().getArea(args[2]);
                        area.setPriority(priority);
                        plugin.getRegionManager().sort();
                        player.sendMessage(StringUtil.color(Message.REGION_PRIORITY_CHANGED.get(player)
                                .replace("%region%", args[2])
                                .replace("%priority%", args[3])));
                        break;
                    }
                    case "set": {
                        if (args.length > 3) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region set <name>", label)));
                            return false;
                        } else if (args.length < 3) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region set <name>", label)));
                            return false;
                        }

                        if (plugin.getRegionManager().exists(args[2])) {
                            Message.DUPLICATED_REGION.send(player);
                            return false;
                        }

                        RegionSelection selection;

                        if (plugin.getVersionManager().getWorldEditProvider() != null) {
                            selection = plugin.getVersionManager().getWorldEditProvider().createSelection(player);

                            if (selection == null) {
                                Message.NO_SELECTION.send(player);
                                return false;
                            }
                        } else {
                            selection = plugin.getRegionManager().getSelection(player);
                        }

                        if (!plugin.getRegionManager().finishSelection(args[2], selection)) {
                            sender.sendMessage(Message.COULD_NOT_CREATE_REGION.get(player));
                            return false;
                        }

                        player.sendMessage(StringUtil.color(Message.SET_REGION.get(player)
                                .replace("%region%", args[2])));
                        return false;
                    }
                    case "delete": {
                        if (args.length > 3) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region delete <name>", label)));
                            return false;
                        } else if (args.length < 3) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region delete <name>", label)));
                            return false;
                        }

                        if (!plugin.getRegionManager().exists(args[2])) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        plugin.getRegionManager().removeArea(args[2]);
                        Message.REMOVE_REGION.send(player);
                        return false;
                    }
                    case "all": {
                        if (args.length > 3) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region all <name>", label)));
                            return false;
                        } else if (args.length < 3) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region all <name>", label)));
                            return false;
                        }

                        RegenerationArea region = plugin.getRegionManager().getArea(args[2]);

                        if (region == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        player.sendMessage(StringUtil.color(String.format(Message.SET_ALL.get(player), region.switchAll() ? "&aall" : "&cnot all")));
                        return false;
                    }
                    case "add": {
                        if (args.length > 4) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region add <name> <preset>", label)));
                            return false;
                        } else if (args.length < 4) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region add <name> <preset>", label)));
                            return false;
                        }

                        RegenerationArea region = plugin.getRegionManager().getArea(args[2]);

                        if (region == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        BlockPreset preset = plugin.getPresetManager().getPreset(args[3]);

                        if (preset == null) {
                            player.sendMessage(Message.INVALID_PRESET.get(player)
                                    .replace("%preset%", args[3]));
                            return false;
                        }

                        if (region.hasPreset(preset.getName())) {
                            player.sendMessage(Message.HAS_PRESET_ALREADY.get(player)
                                    .replace("%region%", args[2])
                                    .replace("%preset%", args[3]));
                            return false;
                        }

                        region.addPreset(preset.getName());
                        player.sendMessage(Message.PRESET_ADDED.get(player)
                                .replace("%preset%", args[3])
                                .replace("%region%", args[2]));
                        break;
                    }
                    case "remove": {
                        if (args.length > 4) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region remove <name> <preset>", label)));
                            return false;
                        } else if (args.length < 4) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region remove <name> <preset>", label)));
                            return false;
                        }

                        RegenerationArea region = plugin.getRegionManager().getArea(args[2]);

                        if (region == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        BlockPreset preset = plugin.getPresetManager().getPreset(args[3]);

                        if (preset == null) {
                            player.sendMessage(Message.INVALID_PRESET.get(player)
                                    .replace("%preset%", args[3]));
                            return false;
                        }

                        if (!region.hasPreset(preset.getName())) {
                            player.sendMessage(Message.DOES_NOT_HAVE_PRESET.get(player)
                                    .replace("%region%", args[2])
                                    .replace("%preset%", args[3]));
                            return false;
                        }

                        region.removePreset(preset.getName());
                        player.sendMessage(Message.PRESET_REMOVED.get(player)
                                .replace("%preset%", args[3])
                                .replace("%region%", args[2]));
                        break;
                    }
                    case "clear": {
                        if (args.length > 3) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region clear <name>", label)));
                            return false;
                        } else if (args.length < 3) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region clear <name>", label)));
                            return false;
                        }

                        RegenerationArea region = plugin.getRegionManager().getArea(args[2]);

                        if (region == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        region.clearPresets();
                        player.sendMessage(Message.PRESETS_CLEARED.get(player)
                                .replace("%region%", region.getName()));
                        break;
                    }
                    case "copy": {
                        if (args.length > 4) {
                            sender.sendMessage(Message.TOO_MANY_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region copy <region-from> <region-to>", label)));
                            return false;
                        } else if (args.length < 4) {
                            sender.sendMessage(Message.NOT_ENOUGH_ARGS.get(player)
                                    .replace("%help%", String.format("/%s region copy <region-from> <region-to>", label)));
                            return false;
                        }

                        RegenerationArea regionFrom = plugin.getRegionManager().getArea(args[2]);

                        if (regionFrom == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        RegenerationArea regionTo = plugin.getRegionManager().getArea(args[3]);

                        if (regionTo == null) {
                            Message.UNKNOWN_REGION.send(player);
                            return false;
                        }

                        regionTo.clearPresets();

                        regionFrom.getPresets().forEach(regionTo::addPreset);
                        player.sendMessage(Message.PRESETS_COPIED.get(player)
                                .replace("%regionFrom%", regionFrom.getName())
                                .replace("%regionTo%", regionTo.getName()));
                        break;
                    }
                    default:
                        sendHelp(sender, label);
                }
                break;
            }
            case "regen": {
                // /blockregen regen -p preset -w world -r region

                if (!sender.hasPermission("blockregen.regen")) {
                    Message.NO_PERM.send(sender);
                    return false;
                }

                String[] workArgs = Arrays.copyOfRange(args, 1, args.length);

                BlockPreset preset = null;
                String worldName = null;
                RegenerationArea region = null;

                Iterator<String> it = Arrays.stream(workArgs).iterator();

                while (it.hasNext()) {
                    String arg = it.next();

                    if (arg.equalsIgnoreCase("-p") && it.hasNext()) {
                        preset = plugin.getPresetManager().getPreset(it.next());
                    } else if (arg.equalsIgnoreCase("-r") && it.hasNext()) {
                        region = plugin.getRegionManager().getArea(it.next());
                    } else if (arg.equalsIgnoreCase("-w") && it.hasNext()) {
                        worldName = it.next();
                    } else {
                        sender.sendMessage(Message.UNKNOWN_ARGUMENT.get());
                        return false;
                    }
                }

                Set<RegenerationProcess> toRegen = new HashSet<>();

                for (RegenerationProcess process : plugin.getRegenerationManager().getCache()) {
                    if ((preset == null || preset.equals(process.getPreset())) &&
                            (region == null || region.getName().equalsIgnoreCase(process.getRegionName())) &&
                            (worldName == null || worldName.equalsIgnoreCase(process.getWorldName()))) {
                        toRegen.add(process);
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> toRegen.forEach(RegenerationProcess::regenerate));

                sender.sendMessage(Message.REGENERATED_PROCESSES.get()
                        .replace("%count%", String.valueOf(toRegen.size())));
                break;
            }
            case "stats": {
                if (!sender.hasPermission("blockregen.admin")) {
                    Message.NO_PERM.send(sender);
                    return false;
                }

                // Compile statistics
                StringBuilder stats = new StringBuilder("&8&m        &r &3BlockRegen processes &8&m        &r\n");

                // Per-world, per-region, per-preset

                /*
                 * world (100):
                 *   region (100): preset (92), preset (20)
                 *   region (200): 24 (preset)
                 *
                 * */

                Collection<RegenerationProcess> processes = plugin.getRegenerationManager().getCache();

                if (processes.isEmpty()) {
                    stats.append("&7None to show.");
                    sender.sendMessage(StringUtil.color(stats.toString()));
                    break;
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Map<String, List<RegenerationProcess>> byWorldCollect = processes.stream()
                            .collect(Collectors.groupingBy(RegenerationProcess::getWorldName));

                    for (Map.Entry<String, List<RegenerationProcess>> byWorld : byWorldCollect.entrySet()) {
                        int byWorldCount = byWorld.getValue().size();

                        stats.append("&f").append(byWorld.getKey()).append(" &7(&f").append(byWorldCount).append("&7)&8:&7\n");

                        Map<String, List<RegenerationProcess>> byRegionCollect = processes.stream()
                                .collect(Collectors.groupingBy(RegenerationProcess::getRegionName));

                        for (Map.Entry<String, List<RegenerationProcess>> byRegion : byRegionCollect.entrySet()) {
                            int byRegionCount = byRegion.getValue().size();

                            stats.append(" &f").append(byRegion.getKey()).append(" &7(&f").append(byRegionCount).append("&7)&8:&7 ");

                            Map<String, List<RegenerationProcess>> byPresetCollect = processes.stream()
                                    .collect(Collectors.groupingBy(RegenerationProcess::getPresetName));

                            for (Map.Entry<String, List<RegenerationProcess>> byPreset : byPresetCollect.entrySet()) {
                                int byPresetCount = byPreset.getValue().size();
                                stats.append("&f").append(byPreset.getKey()).append(" &7(&f").append(byPresetCount).append("&7) ");
                            }
                            stats.append("\n");
                        }
                    }
                    sender.sendMessage(StringUtil.color(stats.toString()));
                });
                break;
            }
            case "debug":
                if (!sender.hasPermission("blockregen.debug")) {
                    Message.NO_PERM.send(sender);
                    return false;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Message.ONLY_PLAYERS.get());
                    return false;
                }

                player = (Player) sender;

                if (plugin.getConsoleHandler().getListeners().contains(sender)) {
                    // Change log level if the debug is not configured.
                    if (!plugin.getFiles().getSettings().getFileConfiguration().getBoolean("Debug-Enabled", false) && plugin.getLogLevel().intValue() <= Level.FINE.intValue()) {
                        plugin.setLogLevel(Level.INFO);
                    }

                    plugin.getConsoleHandler().removeListener(sender);
                    sender.sendMessage(Message.DEBUG_OFF.get(player));
                } else {
                    // Change log level.
                    if (plugin.getLogLevel().intValue() > Level.FINE.intValue()) {
                        plugin.setLogLevel(Level.FINE);
                    }

                    plugin.getConsoleHandler().addListener(sender);
                    sender.sendMessage(Message.DEBUG_ON.get(player));
                }
                break;
            case "discord":
                if (!sender.hasPermission("blockregen.admin")) {
                    Message.NO_PERM.send(sender);
                    return false;
                }

                sender.sendMessage(StringUtil.color(
                                "&8&m        &r &3BlockRegen &f%version% &8&m        &r\n" +
                                        "&7If you need help, either read through the wiki page or reach out on discord!&r\n\n" +
                                        "&3Discord &7https://discord.gg/ZCxMca5&r\n" +
                                        "&6Wiki &7https://github.com/Wertik/BlockRegen/wiki&r\n")
                        .replaceAll("(?i)%version%", plugin.getDescription().getVersion()));
                break;
            case "events":
                if (!sender.hasPermission("blockregen.events")) {
                    sender.sendMessage(Message.NO_PERM.get());
                    return false;
                }

                if (args.length < 3) {

                    if (plugin.getEventManager().getLoadedEvents().isEmpty()) {
                        sender.sendMessage(StringUtil.color("&8&m     &r &3BlockRegen Events &8&m     "
                                + "\n&cYou haven't made any events yet."
                                + "\n&8&m                       "));
                        return false;
                    }

                    StringBuilder list = new StringBuilder("&8&m     &r &3BlockRegen Events &8&m     \n" +
                            "&7You have the following events loaded:").append("\n&r ");

                    for (PresetEvent event : plugin.getEventManager().getEvents(e -> true)) {
                        list.append("\n&8 - &r").append(event.getDisplayName()).append(" &7(Name: &f")
                                .append(event.getName()).append("&7) ")
                                .append(event.isEnabled() ? " &a(active)&r" : " &c(inactive)&r");
                    }

                    list.append("\n&r \n&7Use &3/").append(label).append(" events activate <name> &7to activate it.\n")
                            .append("&7Use &3/").append(label).append(" events deactivate <name> &7to de-activate it.");
                    sender.sendMessage(StringUtil.color(list.toString()));
                } else {
                    if (args[1].equalsIgnoreCase("activate")) {
                        String name = args[2];

                        PresetEvent event = plugin.getEventManager().getEvent(name);

                        if (event == null) {
                            sender.sendMessage(Message.EVENT_NOT_FOUND.get());
                            return false;
                        }

                        if (event.isEnabled()) {
                            sender.sendMessage(Message.EVENT_ALREADY_ACTIVE.get());
                            return false;
                        }

                        plugin.getEventManager().enableEvent(event);
                        sender.sendMessage(StringUtil
                                .color(Message.ACTIVATE_EVENT.get().replace("%event%", event.getDisplayName())));
                        return false;
                    }

                    if (args[1].equalsIgnoreCase("deactivate")) {
                        String name = args[2];

                        PresetEvent event = plugin.getEventManager().getEvent(name);

                        if (event == null) {
                            sender.sendMessage(Message.EVENT_NOT_FOUND.get());
                            return false;
                        }

                        if (!event.isEnabled()) {
                            sender.sendMessage(Message.EVENT_NOT_ACTIVE.get());
                            return false;
                        }

                        plugin.getEventManager().disableEvent(event);
                        sender.sendMessage(StringUtil
                                .color(Message.DEACTIVATE_EVENT.get().replace("%event%", event.getDisplayName())));
                        return false;
                    }
                }
                break;
            default: {
                sendHelp(sender, label);
            }
        }
        return false;
    }

    private void giveTools(@NotNull Player player) {
        ItemStack shovel = XMaterial.WOODEN_SHOVEL.parseItem();

        ItemMeta meta = shovel.getItemMeta();
        meta.setDisplayName(StringUtil.color("&3BlockRegen preset tool"));
        meta.setLore(Lists.newArrayList(StringUtil.color(
                "&fLeft click &7on a block in a region to add the blocks preset.",
                "&fRight click &7on a block in a region to remove the blocks preset.")));
        shovel.setItemMeta(meta);

        ItemStack axe = XMaterial.WOODEN_AXE.parseItem();

        meta = axe.getItemMeta();
        meta.setDisplayName(StringUtil.color("&3BlockRegen selection tool"));
        meta.setLore(Lists.newArrayList(StringUtil.color("&fLeft click &7to select first position.",
                "&fRight click &7to select second position.",
                "&f/blockregen region set <name> &7to create a region from selection.")));
        axe.setItemMeta(meta);

        player.getInventory().addItem(shovel, axe);

        Message.TOOLS.send(player);
    }

    private void listRegions(CommandSender sender) {
        StringBuilder message = new StringBuilder("&8&m    &3 BlockRegen Regions &8&m    &r\n");
        for (RegenerationArea area : plugin.getRegionManager().getLoadedAreas()) {

            message.append(String.format(" &f%s\n", area.getName()));

            if (area instanceof RegenerationRegion) {
                RegenerationRegion region = (RegenerationRegion) area;
                message.append(String.format("  &7Area: &f%s &8- &f%s", LocationUtil.locationToString(region.getMin()), LocationUtil.locationToString(region.getMax()))).append('\n');
            } else if (area instanceof RegenerationWorld) {
                RegenerationWorld world = (RegenerationWorld) area;
                message.append("  &7World: &f").append(world.getWorldName()).append('\n');
            }

            message.append(String.format("  &7Priority: &f%d\n", area.getPriority()));

            if (area.isAll()) {
                message.append("  &7Presets: &aall\n");
            } else {
                if (!area.getPresets().isEmpty()) {
                    message.append("  &7Presets: ").append(String.format("&f%s\n", area.getPresets()));
                } else {
                    message.append("  &7Presets: &cnone\n");
                }
            }
        }
        sender.sendMessage(StringUtil.color(message.toString()));
    }

    private boolean checkConsole(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.ONLY_PLAYERS.get());
            return true;
        }

        return false;
    }
}