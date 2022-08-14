package nl.aurorion.blockregen.util;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class TextUtil {

    public String parse(String string) {

        if (Strings.isNullOrEmpty(string)) {
            return string;
        }

        string = string.replaceAll("(?i)%prefix%", Message.PREFIX.getValue());
        return string;
    }

    public String parse(String string, @Nullable Player player) {
        if (Strings.isNullOrEmpty(string)) {
            return string;
        }

        string = parse(string);

        if (player == null) {
            return string;
        }

        string = string.replaceAll("(?i)%player%", player.getName());

        if (BlockRegen.getInstance().isUsePlaceholderAPI()) {
            string = PlaceholderAPI.setPlaceholders(player, string);
        }

        return string;
    }

    public String capitalizeWord(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String capitalize(String str) {
        return Arrays.stream(str.split(" "))
                .map(TextUtil::capitalizeWord)
                .collect(Collectors.joining(" "));
    }
}
