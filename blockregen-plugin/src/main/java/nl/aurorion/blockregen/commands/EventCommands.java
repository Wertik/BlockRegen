package nl.aurorion.blockregen.commands;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.raincloud.CommandManager;
import nl.aurorion.blockregen.raincloud.ValueParser;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;

import java.util.ArrayList;

public class EventCommands extends CommandSet {

    public EventCommands(BlockRegen plugin, CommandManager manager) {
        super(plugin, manager);
    }

    public void register() {
        manager.command("blockregen", "Activate an event.")
                .literal("events")
                .literal("activate")
                .required("event", "Event to activate.",
                        ValueParser.stringParser(),
                        (context, args) -> new ArrayList<>(plugin.getEventManager().getLoadedEvents().keySet()))
                .permission("blockregen.events")
                .handler(context -> {
                    PresetEvent event = plugin.getEventManager().getEvent(context.get("event"));

                    if (event == null) {
                        context.sender().sendMessage(Message.EVENT_NOT_FOUND.get());
                        return;
                    }

                    if (event.isEnabled()) {
                        context.sender().sendMessage(Message.EVENT_ALREADY_ACTIVE.get());
                        return;
                    }

                    plugin.getEventManager().enableEvent(event);
                    context.sender().sendMessage(StringUtil.color(Message.ACTIVATE_EVENT.get().placeholder("%event%", event.getDisplayName())));
                });

        manager.command("blockregen", "Deactivate an event.")
                .literal("events")
                .literal("deactivate")
                .required("event", "Event to deactivate.",
                        ValueParser.stringParser(),
                        (context, args) -> new ArrayList<>(plugin.getEventManager().getLoadedEvents().keySet()))
                .permission("blockregen.events")
                .handler(context -> {
                    PresetEvent event = plugin.getEventManager().getEvent(context.get("event"));

                    if (event == null) {
                        context.sender().sendMessage(Message.EVENT_NOT_FOUND.get());
                        return;
                    }

                    if (!event.isEnabled()) {
                        context.sender().sendMessage(Message.EVENT_NOT_ACTIVE.get());
                        return;
                    }

                    plugin.getEventManager().disableEvent(event);
                    context.sender().sendMessage(StringUtil.color(Message.DEACTIVATE_EVENT.get().placeholder("%event%", event.getDisplayName())));
                });
    }
}
