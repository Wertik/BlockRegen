package nl.aurorion.blockregen.commands;

import com.google.common.collect.Lists;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.raincloud.CommandManager;
import nl.aurorion.blockregen.raincloud.ValueParser;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.system.region.struct.RegionSelection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class RegionCommands extends CommandSet {

    public RegionCommands(BlockRegen plugin, CommandManager manager) {
        super(plugin, manager);
    }

    public void register() {
        manager.command("blockregen", "List regions.")
                .literal("regions")
                .permission("blockregen.region")
                .handler(context -> listRegions(context.sender()));

        manager.command("blockregen", "List regions.")
                .literal("region")
                .literal("list")
                .permission("blockregen.region")
                .handler(context -> listRegions(context.sender()));

        manager.command("blockregen", "Create a region.")
                .literal("region")
                .literal("create", "set")
                .required("region", "Name of the region to create.", ValueParser.stringParser(), null)
                .permission("blockregen.region").senderPlayer()
                .handler(context -> {
                    Player player = context.sender();

                    String name = context.get("name");

                    if (plugin.getRegionManager().exists(name)) {
                        Message.DUPLICATED_REGION.send(player);
                        return;
                    }

                    RegionSelection selection;
                    if (plugin.getVersionManager().getWorldEditProvider() != null) {
                        selection = plugin.getVersionManager().getWorldEditProvider().createSelection(player);

                        if (selection == null) {
                            Message.NO_SELECTION.send(player);
                            return;
                        }
                    } else {
                        selection = plugin.getRegionManager().getSelection(player);
                    }

                    if (!plugin.getRegionManager().finishSelection(name, selection)) {
                        player.sendMessage(Message.COULD_NOT_CREATE_REGION.get(player));
                        return;
                    }

                    player.sendMessage(StringUtil.color(Message.SET_REGION.get(player).replace("%region%", name)));
                });

        manager.command("blockregen", "Delete a region.")
                .literal("region")
                .literal("delete")
                .required("region", "Name of the region to delete.", this.regionParser, this.regionProvider)
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationArea region = context.get("region");
                    plugin.getRegionManager().removeArea(region.getName());
                    Message.REMOVE_REGION.send(context.sender());
                });

        manager.command("blockregen", "Enable all presets for this region.")
                .literal("region")
                .literal("all")
                .required("region", "Name of the region to switch the option for.", this.regionParser, this.regionProvider)
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationArea region = context.get("region");
                    context.sender().sendMessage(StringUtil.color(String.format(Message.SET_ALL.get(), region.switchAll() ? "&aall" : "&cnot all")));
                });

        manager.command("blockregen", "Add preset to region.")
                .literal("region")
                .literal("add")
                .required("region", "Name of the region to switch the option for.", this.regionParser, this.regionProvider)
                .required("preset", "Name of the preset to add.", this.presetParser, (sender, args) -> Lists.newArrayList(plugin.getPresetManager().getPresets().keySet()))
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationArea region = context.get("region");
                    BlockPreset preset = context.get("preset");

                    if (region.hasPreset(preset.getName())) {
                        context.sender().sendMessage(Message.HAS_PRESET_ALREADY.get().replace("%region%", region.getName()).replace("%preset%", preset.getName()));
                        return;
                    }

                    region.addPreset(preset.getName());
                    context.sender().sendMessage(Message.PRESET_ADDED.get().replace("%preset%", preset.getName()).replace("%region%", region.getName()));
                });

        manager.command("blockregen", "Remove preset to region.")
                .literal("region")
                .literal("remove")
                .required("region", "Region to remove the preset from.", this.regionParser, this.regionProvider)
                .required("preset", "Name of the preset to add.", this.presetParser, (context, args) -> {
                    RegenerationArea region = context.get("region");
                    return new ArrayList<>(region.getPresets());
                })
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationArea region = context.get("region");
                    BlockPreset preset = context.get("preset");

                    if (!region.hasPreset(preset.getName())) {
                        context.sender().sendMessage(Message.DOES_NOT_HAVE_PRESET.get().replace("%region%", region.getName()).replace("%preset%", preset.getName()));
                        return;
                    }

                    region.removePreset(preset.getName());
                    context.sender().sendMessage(Message.PRESET_REMOVED.get().replace("%preset%", preset.getName()).replace("%region%", region.getName()));
                });

        manager.command("blockregen", "Region to clear presets from.")
                .literal("region")
                .literal("clear")
                .required("region", "Region to clear the preset from.", this.regionParser, this.regionProvider)
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationArea region = context.get("region");
                    region.clearPresets();
                    context.sender().sendMessage(Message.PRESETS_CLEARED.get().replace("%region%", region.getName()));
                });

        manager.command("blockregen", "Copy preset settings from one region to another.")
                .literal("region")
                .literal("copy")
                .required("from", "Region to copy presets from.", this.regionParser, this.regionProvider)
                .required("to", "Region to copy presets to.", this.regionParser, (context, args) -> Lists.newArrayList(plugin.getRegionManager().getLoadedAreas().stream().map(RegenerationArea::getName).filter(r -> {
                    RegenerationRegion from = context.get("from");
                    return !Objects.equals(r, from.getName());
                }).collect(Collectors.toSet())))
                .permission("blockregen.region")
                .handler(context -> {
                    RegenerationRegion from = context.get("from");
                    RegenerationRegion to = context.get("to");

                    to.clearPresets();

                    from.getPresets().forEach(to::addPreset);
                    context.sender().sendMessage(Message.PRESETS_COPIED.get().replace("%regionFrom%", from.getName()).replace("%regionTo%", to.getName()));
                });
    }

    private void listRegions(CommandSender sender) {
        StringBuilder message = new StringBuilder("&8&m    &3 BlockRegen Regions &8&m    &r\n");
        for (RegenerationArea area : plugin.getRegionManager().getLoadedAreas()) {

            message.append(String.format("&8  - &f%s", area.getName()));

            if (area.isAll()) {
                message.append("&8 (&aall&8)\n");
            } else {
                if (!area.getPresets().isEmpty()) {
                    message.append(String.format("&8 (&r%s&8)\n", area.getPresets()));
                } else {
                    message.append("&8 (&cnone&8)\n");
                }
            }
        }
        sender.sendMessage(StringUtil.color(message.toString()));
    }
}
