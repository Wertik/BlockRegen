package nl.aurorion.blockregen;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class StringUtil {
    public String stripColor(String msg) {
        return msg != null ? ChatColor.stripColor(msg) : null;
    }

    @NotNull
    public String color(@Nullable String msg) {
        return color(msg, '&');
    }

    @NotNull
    public String color(@Nullable String msg, char colorChar) {
        return msg == null ? "" : ChatColor.translateAlternateColorCodes(colorChar, msg);
    }
}
