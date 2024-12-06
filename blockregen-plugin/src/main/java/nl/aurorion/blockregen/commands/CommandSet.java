package nl.aurorion.blockregen.commands;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.raincloud.CommandManager;
import nl.aurorion.blockregen.raincloud.ParseException;
import nl.aurorion.blockregen.raincloud.ValueParser;
import nl.aurorion.blockregen.raincloud.argument.SuggestionProvider;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;

import java.util.stream.Collectors;

public abstract class CommandSet {

    protected final BlockRegen plugin;
    protected final CommandManager manager;

    protected final ValueParser<RegenerationArea> regionParser;
    protected final ValueParser<BlockPreset> presetParser;

    protected final SuggestionProvider regionProvider;

    public CommandSet(BlockRegen plugin, CommandManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        this.regionParser = (input) -> {
            RegenerationArea region = this.plugin.getRegionManager().getArea(input);

            if (region == null) {
                throw new ParseException(null, input, Message.UNKNOWN_REGION.get());
            }

            return region;
        };

        this.presetParser = (input) -> {
            BlockPreset preset = this.plugin.getPresetManager().getPreset(input);

            if (preset == null) {
                throw new ParseException(null, input, Message.INVALID_PRESET.get().replace("%preset%", input));
            }

            return preset;
        };

        this.regionProvider = (context, args) -> plugin.getRegionManager().getLoadedAreas().stream().map(RegenerationArea::getName).collect(Collectors.toList());
    }

    public abstract void register();
}
