package nl.aurorion.blockregen.raincloud.argument;

import nl.aurorion.blockregen.raincloud.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public interface SuggestionProvider {

    List<String> provide(CommandContext<?> context, String[] args);

    static PlayerNameProvider playerNameProvider() {
        return new PlayerNameProvider();
    }

    class PlayerNameProvider implements SuggestionProvider {
        @Override
        public List<String> provide(CommandContext<?> context, String[] args) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
    }
}
