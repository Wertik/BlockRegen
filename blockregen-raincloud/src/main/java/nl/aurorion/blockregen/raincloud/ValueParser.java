package nl.aurorion.blockregen.raincloud;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface ValueParser<O> {

    O parse(String input) throws ParseException;

    class StringParser implements ValueParser<String> {
        @Override
        public String parse(String input) throws ParseException {
            return input;
        }
    }

    class PlayerParser implements ValueParser<Player> {
        @Override
        public Player parse(String input) throws ParseException {
            OfflinePlayer offlinePlayer = Bukkit.getPlayer(input);

            if (offlinePlayer == null) {
                throw new ParseException(null, input, "Player not found.");
            }

            if (!offlinePlayer.isOnline()) {
                throw new ParseException(null, input, "Player is not online.");
            }

            return offlinePlayer.getPlayer();
        }
    }

    static ValueParser.StringParser stringParser() {
        return new StringParser();
    }

    static ValueParser.PlayerParser playerParser() {
        return new PlayerParser();
    }
}
