package nl.aurorion.blockregen.util;

import com.google.common.base.Strings;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.Message;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Text {

    private static final Map<String, Pattern> PATTERNS = new HashMap<>();

    @NotNull
    public static Pattern getPattern(@NotNull String placeholder) {
        Pattern pattern = PATTERNS.get(placeholder);
        if (pattern == null) {
            pattern = Pattern.compile("(?i)%" + placeholder + "%");
            PATTERNS.put(placeholder, pattern);
        }
        return pattern;
    }

    @Contract("null,_,_->null")
    public static String replace(String text, @NotNull String placeholder, Object value) {
        if (Strings.isNullOrEmpty(text)) {
            return text;
        }
        return getPattern(placeholder).matcher(text).replaceAll(String.valueOf(value));
    }

    // Parse placeholders with different objects as context.
    public static String parse(String string, Object... context) {
        if (Strings.isNullOrEmpty(string)) {
            return string;
        }

        string = getPattern("prefix").matcher(string).replaceAll(Message.PREFIX.getValue());

        for (Object o : context) {
            if (o instanceof Player) {
                Player player = (Player) o;
                string = getPattern("player").matcher(string).replaceAll(player.getName());
                if (BlockRegenPluginImpl.getInstance().isUsePlaceholderAPI()) {
                    string = PlaceholderAPI.setPlaceholders((Player) o, string);
                }
                string = Text.replace(string, "player_x", Math.round(player.getLocation().getX()));
                string = Text.replace(string, "player_y", Math.round(player.getLocation().getY()));
                string = Text.replace(string, "player_z", Math.round(player.getLocation().getZ()));
                string = Text.replace(string, "player_world", player.getLocation().getWorld().getName());
            } else if (o instanceof Block) {
                Block block = (Block) o;
                string = Text.replace(string, "block_x", block.getLocation().getBlockX());
                string = Text.replace(string, "block_y", block.getLocation().getBlockY());
                string = Text.replace(string, "block_z", block.getLocation().getBlockZ());
                string = Text.replace(string, "block_world", block.getWorld().getName());
            }
        }

        return string;
    }

    public static String parse(String string) {
        return parse(string, new Object[]{});
    }

    public static String capitalizeWord(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String capitalize(String str) {
        return Arrays.stream(str.split(" "))
                .map(Text::capitalizeWord)
                .collect(Collectors.joining(" "));
    }
}
