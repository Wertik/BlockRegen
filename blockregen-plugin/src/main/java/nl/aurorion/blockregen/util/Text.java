package nl.aurorion.blockregen.util;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.Message;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class Text {

    // Parse placeholders with different objects as context.
    public String parse(String string, Object... context) {
        if (Strings.isNullOrEmpty(string)) {
            return string;
        }

        string = string.replaceAll("(?i)%prefix%", Message.PREFIX.getValue());

        for (Object o : context) {
            if (o instanceof Player) {
                Player player = (Player) o;
                string = string.replaceAll("(?i)%player%", player.getName());
                if (BlockRegenPluginImpl.getInstance().isUsePlaceholderAPI()) {
                    string = PlaceholderAPI.setPlaceholders((Player) o, string);
                }
            } else if (o instanceof Block) {
                Block block = (Block) o;
                string = string.replaceAll("(?i)%block_x%", String.valueOf(block.getLocation().getBlockX()));
                string = string.replaceAll("(?i)%block_y%", String.valueOf(block.getLocation().getBlockY()));
                string = string.replaceAll("(?i)%block_z%", String.valueOf(block.getLocation().getBlockZ()));
                string = string.replaceAll("(?i)%block_world%", block.getWorld().getName());
            }
        }

        return string;
    }

    public String parse(String string) {
        return parse(string, new Object[]{});
    }

    public String capitalizeWord(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String capitalize(String str) {
        return Arrays.stream(str.split(" "))
                .map(Text::capitalizeWord)
                .collect(Collectors.joining(" "));
    }
}
