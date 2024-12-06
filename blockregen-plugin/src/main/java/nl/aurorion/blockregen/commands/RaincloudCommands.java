package nl.aurorion.blockregen.commands;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.raincloud.CommandManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Log
public class RaincloudCommands {

    private final BlockRegen plugin;

    private final List<CommandSet> sets;

    @Getter
    private final CommandManager manager = new CommandManager((sender, key) -> {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return Message.get(key, player);
        }
        return Message.get(key, null);
    });

    public RaincloudCommands(BlockRegen plugin) {
        this.plugin = plugin;
        this.sets = Arrays.asList(
                new AdminCommands(this.plugin, this.manager),
                new EventCommands(this.plugin, this.manager),
                new RegionCommands(this.plugin, this.manager)
        );
    }

    public void register() {
        this.sets.forEach(CommandSet::register);

        Objects.requireNonNull(this.plugin.getCommand("blockregen")).setExecutor(manager);
        Objects.requireNonNull(this.plugin.getCommand("blockregen")).setTabCompleter(manager);
    }
}
