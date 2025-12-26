package nl.aurorion.blockregen.preset;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.preset.drop.DropItem;
import nl.aurorion.blockregen.util.Text;
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
        AtomicDouble money = new AtomicDouble(0.0);

        BlockRegenPluginImpl.getInstance().getCompatibilityManager().getEconomy().ifLoaded((economy) -> {
            money.set(this.money.getDouble());
            double m = money.get();
            if (m > 0) {
                economy.depositPlayer(player, m);
            }
        });

        final Function<String, String> finalParser = (string) -> Text.replace(parser.apply(string), "earned_money", money.get());

        Bukkit.getScheduler().runTask(BlockRegenPluginImpl.getInstance(), () -> {
            for (Command command : playerCommands) {
                if (command.shouldExecute()) {
                    Bukkit.dispatchCommand(player, finalParser.apply(command.getCommand()));
                }
            }

            for (Command command : consoleCommands) {
                if (command.shouldExecute()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalParser.apply(command.getCommand()));
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

        for (String strCmd : strCommands) {
            if (strCmd.contains(";")) {
                // Split around any extra spaces.
                // 50 ; say Hello
                String[] args = strCmd.split(" *; *");

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