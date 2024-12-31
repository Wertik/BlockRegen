package nl.aurorion.blockregen.commands;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.raincloud.CommandManager;
import nl.aurorion.blockregen.raincloud.ValueParser;
import nl.aurorion.blockregen.raincloud.argument.SuggestionProvider;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class AdminCommands extends CommandSet {

    public AdminCommands(BlockRegen plugin, CommandManager manager) {
        super(plugin, manager);
    }

    @Override
    public void register() {
        manager.command("blockregen", "Help page.")
                .handler(context -> {
                    if (manager.getCommands().stream()
                            .noneMatch(c -> (c.getPermission() == null || context.sender().hasPermission(c.getPermission())) && c != context.command())) {
                        // todo: lang
                        context.sender().sendMessage(StringUtil.color("&cNo commands for you."));
                        return;
                    }

                    context.sender().sendMessage(StringUtil.color(TextUtil.parse("&8&m        &r &3BlockRegen &f%version% &8&m        &r\n" +
                            manager.composeHelp(context.sender(), context.label()))));
                });

        manager.command("blockregen", "Reload the plugin.")
                .literal("reload")
                .permission("blockregen.reload")
                .handler(context -> {
                    plugin.reload(context.sender());
                });

        manager.command("blockregen", "Receive debug messages in chat.")
                .literal("debug")
                .optional("player", "Player to target.", ValueParser.playerParser(), SuggestionProvider.playerNameProvider())
                .onParseException((context, exception) -> {
                    context.sender().sendMessage(StringUtil.color("%prefix% &c" + exception.getMessage()));
                })
                .senderPlayer()
                .permission("blockregen.debug")
                .handler(context -> {
                    Player player = context.sender();
                    Player target = context.getOrDefault("player", player);

                    if (plugin.getConsoleHandler().getListeners().contains(target)) {
                        // Change log level if the debug is not configured.
                        if (!plugin.getFiles().getSettings().getFileConfiguration().getBoolean("Debug-Enabled", false) && plugin.getLogLevel().intValue() <= Level.FINE.intValue()) {
                            plugin.setLogLevel(Level.INFO);
                        }

                        plugin.getConsoleHandler().removeListener(target);
                        player.sendMessage(Message.DEBUG_OFF.get(player));
                    } else {
                        // Change log level.
                        if (plugin.getLogLevel().intValue() > Level.FINE.intValue()) {
                            plugin.setLogLevel(Level.FINE);
                        }

                        plugin.getConsoleHandler().addListener(target);
                        player.sendMessage(Message.DEBUG_ON.get(player));
                    }
                });

        manager.command("blockregen", "Get useful links for configuring this plugin.")
                .literal("help")
                .permission("blockregen.admin")
                .handler(context -> context.sender().sendMessage(StringUtil.color(
                                "&8&m        &r &3BlockRegen &f%version% &8&m        &r\n" +
                                        "&7If you need help, either read through the wiki page or reach out on discord!&r\n\n" +
                                        "&3Discord &7https://discord.gg/ZCxMca5&r\n" +
                                        "&6Wiki &7https://github.com/Wertik/BlockRegen/wiki&r\n")
                        .replaceAll("(?i)%version%", this.plugin.getDescription().getVersion())));

        manager.command("blockregen", "Turn on bypass.")
                .literal("bypass")
                .optional("player", "Player to target.",
                        ValueParser.playerParser(),
                        SuggestionProvider.playerNameProvider())
                .permission("blockregen.bypass")
                .senderPlayer()
                .handler(context -> {
                    Player player = context.sender();

                    Player target = context.getOrDefault("player", player);

                    if (plugin.getRegenerationManager().switchBypass(target)) {
                        Message.BYPASS_ON.send(player);
                    } else {
                        Message.BYPASS_OFF.send(player);
                    }
                });

        manager.command("blockregen", "Turn on check.")
                .literal("check")
                .optional("player", "Player to target.", ValueParser.playerParser(), SuggestionProvider.playerNameProvider())
                .onParseException((context, exception) -> {
                    context.sender().sendMessage(StringUtil.color("%prefix% &c" + exception.getMessage()));
                })
                .permission("blockregen.check")
                .senderPlayer()
                .handler(context -> {
                    Player player = context.sender();
                    Player target = context.getOrDefault("player", player);

                    if (plugin.getRegenerationManager().switchBypass(target)) {
                        Message.DATA_CHECK_ON.send(player);
                    } else {
                        Message.DATA_CHECK_OFF.send(player);
                    }
                });

        manager.command("blockregen", "Get tools.")
                .literal("tools")
                .senderPlayer()
                .permission("blockregen.tools")
                .handler(context -> {
                    this.giveTools(context.sender());
                    Message.TOOLS.send(context.sender());
                });

        manager.command("blockregen", "Regenerate running processes.")
                .literal("regen").flag("region", "Region filter.",
                        ValueParser.stringParser(),
                        this.regionProvider)
                .flag("preset", "Preset filter.",
                        ValueParser.stringParser(),
                        (sender, args) -> Lists.newArrayList(plugin.getPresetManager().getPresets().keySet()))
                .flag("world", "World filter.",
                        ValueParser.stringParser(),
                        (sender, args) -> Bukkit.getWorlds().stream()
                                .map(World::getName)
                                .collect(Collectors.toList()))
                .permission("blockregen.regen")
                .handler(context -> {
                    BlockPreset preset = null;
                    String worldName = context.get("world");
                    RegenerationArea region = null;

                    String presetName = context.get("preset");
                    if (presetName != null) {
                        preset = plugin.getPresetManager().getPreset(presetName);

                        if (preset == null) {
                            context.sender().sendMessage(Message.INVALID_PRESET.get());
                            return;
                        }
                    }

                    String regionName = context.get("region");
                    if (regionName != null) {
                        region = plugin.getRegionManager().getArea(regionName);

                        if (region == null) {
                            context.sender().sendMessage(Message.UNKNOWN_REGION.get());
                            return;
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

                    toRegen.forEach(RegenerationProcess::regenerate);

                    context.sender().sendMessage(Message.REGENERATED_PROCESSES.get().placeholder("%count%", String.valueOf(toRegen.size())));
                });

        manager.command("blockregen", "List available events.")
                .literal("events")
                .permission("blockregen.events")
                .handler(context -> {
                    if (plugin.getEventManager().getLoadedEvents().isEmpty()) {
                        context.sender().sendMessage(StringUtil.color(
                                "&8&m     &r &3BlockRegen Events &8&m     \n\n" +
                                        "&cYou haven't made any events yet.&r\n"));
                        return;
                    }

                    StringBuilder list = new StringBuilder("&8&m     &r &3BlockRegen Events &8&m     \n" + "&7You have the following events loaded:").append("\n&r ");

                    for (PresetEvent event : plugin.getEventManager().getEvents(e -> true)) {
                        list.append("\n&8 - &r").append(event.getDisplayName()).append(" &7(Name: &f").append(event.getName()).append("&7) ").append(event.isEnabled() ? " &a(active)&r" : " &c(inactive)&r");
                    }

                    list.append("\n&r \n&7Use &3/").append(context.label()).append(" events activate <name> &7to activate it.\n").append("&7Use &3/").append(context.label()).append(" events deactivate <name> &7to de-activate it.");
                    context.sender().sendMessage(StringUtil.color(list.toString()));
                });
    }

    private void giveTools(Player player) {
        ItemStack shovel = XMaterial.WOODEN_SHOVEL.parseItem();

        if (shovel == null) {
            log.warning("Failed to parse material " + XMaterial.WOODEN_SHOVEL.name() + " into an item.");
            player.sendMessage(StringUtil.color(TextUtil.parse("%prefix% Something went wrong.")));
            return;
        }

        ItemMeta meta = shovel.getItemMeta();
        meta.setDisplayName(StringUtil.color("&3BlockRegen preset tool"));
        meta.setLore(Lists.newArrayList(StringUtil.color("&fLeft click &7on a block in a region to add the blocks preset.", "&fRight click &7on a block in a region to remove the blocks preset.")));
        shovel.setItemMeta(meta);

        ItemStack axe = XMaterial.WOODEN_AXE.parseItem();

        if (axe == null) {
            log.warning("Failed to parse material " + XMaterial.WOODEN_AXE.name() + " into an item.");
            player.sendMessage(StringUtil.color(TextUtil.parse("%prefix% Something went wrong.")));
            return;
        }

        meta = axe.getItemMeta();
        meta.setDisplayName(StringUtil.color("&3BlockRegen selection tool"));
        meta.setLore(Lists.newArrayList(StringUtil.color("&fLeft click &7to select first position.", "&fRight click &7to select second position.", "&f/blockregen region set <name> &7to create a region from selection.")));
        axe.setItemMeta(meta);

        player.getInventory().addItem(shovel, axe);
    }
}
