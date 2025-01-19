package nl.aurorion.blockregen.preset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.preset.drop.DropItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Log
@NoArgsConstructor
public class PresetRewards {

    @Getter
    @Setter
    private NumberValue money;

    @Getter
    private List<Command> consoleCommands;

    @Getter
    private List<Command> playerCommands;

    @Getter
    private List<DropItem> drops = new ArrayList<>();

    public void give(Player player, Function<String, String> parser) {
        if (BlockRegenPluginImpl.getInstance().getCompatibilityManager().getEconomy().isLoaded()) {
            double money = this.money.getDouble();
            if (money > 0) {
                BlockRegenPluginImpl.getInstance().getCompatibilityManager().getEconomy().get().depositPlayer(player, money);
            }
        }

        // Sync commands
        Bukkit.getScheduler().runTask(BlockRegenPluginImpl.getInstance(), () -> {
            for (Command command : playerCommands) {
                if (command.shouldExecute()) {
                    Bukkit.dispatchCommand(player, parser.apply(command.getCommand()));
                }
            }

            for (Command command : consoleCommands) {
                if (command.shouldExecute()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parser.apply(command.getCommand()));
                }
            }
        });
    }

    public void parseConsoleCommands(@NotNull List<String> consoleCommands) {
        this.consoleCommands = this.parseCommands(consoleCommands);
    }

    public void parsePlayerCommands(@NotNull List<String> playerCommands) {
        this.playerCommands = this.parseCommands(playerCommands);
    }

    private List<Command> parseCommands(@NotNull List<String> strCommands) {
        List<Command> commands = new ArrayList<>();
        // Parse the input.
        for (String strCmd : strCommands) {
            if (strCmd.contains(";")) {
                String[] args = strCmd.split(";");

                double chance;

                try {
                    chance = Double.parseDouble(args[0]);
                } catch (NumberFormatException e) {
                    log.warning(String.format("Invalid number format for input %s in command %s", args[0], strCmd));
                    continue;
                }

                if (args[1].trim().isEmpty()) {
                    continue;
                }

                commands.add(new Command(args[1], chance));
            } else {
                if (strCmd.trim().isEmpty()) {
                    continue;
                }

                commands.add(new Command(strCmd, 100));
            }
        }
        return commands;
    }

    public void setDrops(List<DropItem> drops) {
        this.drops = drops == null ? new ArrayList<>() : drops;
    }

    @Override
    public String toString() {
        return "PresetRewards{" +
                "money=" + money +
                ", consoleCommands=" + consoleCommands +
                ", playerCommands=" + playerCommands +
                ", drops=" + drops +
                '}';
    }
}