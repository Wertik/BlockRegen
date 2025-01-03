package nl.aurorion.blockregen.system.preset.struct;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.system.preset.struct.drop.ItemDrop;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log
@NoArgsConstructor
public class PresetRewards {

    @Getter
    @Setter
    private Amount money;

    @Getter
    private List<Command> consoleCommands;

    @Getter
    private List<Command> playerCommands;

    @Getter
    private List<ItemDrop> drops = new ArrayList<>();

    @NotNull
    public static PresetRewards load(@Nullable ConfigurationSection section, BlockPreset preset) {

        if (section == null)
            return new PresetRewards();

        PresetRewards rewards = new PresetRewards();

        rewards.parseConsoleCommands(
                getStringOrList(section, "console-commands", "console-command", "commands", "command"));
        rewards.parsePlayerCommands(getStringOrList(section, "player-commands", "player-command"));
        rewards.setMoney(Amount.load(section, "money", 0));

        ConfigurationSection dropSection = section.getConfigurationSection("drop-item");

        // Items Drops
        if (dropSection != null) {
            // Single drop
            if (dropSection.contains("material")) {
                XMaterial material = ParseUtil.parseMaterial(dropSection.getString("material"));

                if (material != null) {
                    ItemDrop drop = ItemDrop.load(dropSection, preset);
                    if (drop != null) {
                        rewards.getDrops().add(drop);
                    }
                } else {
                    log.warning("Could not load item drop at " + dropSection.getCurrentPath()
                            + ".drop-item, material is invalid.");
                }
            } else {
                // Multiple drops
                for (String dropName : dropSection.getKeys(false)) {
                    ItemDrop drop = ItemDrop.load(dropSection.getConfigurationSection(dropName), preset);
                    if (drop != null) {
                        rewards.getDrops().add(drop);
                    }
                }
            }

            log.fine(() -> "Loaded drops " + rewards.getDrops().stream().map(ItemDrop::toString).collect(Collectors.joining()));
        }
        return rewards;
    }

    @NotNull
    private static List<String> getStringOrList(ConfigurationSection section, String... keys) {
        for (String key : keys) {

            if (section.get(key) == null) {
                continue;
            }

            if (section.isList(key)) {
                return section.getStringList(key);
            } else if (section.isString(key)) {
                String str = section.getString(key);
                return Collections.singletonList(str);
            }
        }
        return new ArrayList<>();
    }

    public void give(Player player, Function<String, String> parser) {

        if (BlockRegen.getInstance().getEconomy() != null) {
            double money = this.money.getDouble();
            if (money > 0) {
                BlockRegen.getInstance().getEconomy().depositPlayer(player, money);
            }
        }

        // Sync commands
        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> {
            playerCommands.stream().filter(Command::shouldExecute).forEach(command -> Bukkit.dispatchCommand(player, parser.apply(command.getCommand())));
            consoleCommands.stream().filter(Command::shouldExecute).forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parser.apply(command.getCommand())));
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

    public void setDrops(List<ItemDrop> drops) {
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