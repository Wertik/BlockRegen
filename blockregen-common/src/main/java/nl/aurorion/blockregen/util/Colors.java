package nl.aurorion.blockregen.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Colors {

    @Contract("null->null")
    public static String stripColor(@Nullable String msg) {
        return msg != null ? ChatColor.stripColor(msg) : null;
    }

    @NotNull
    public static String color(@Nullable String msg) {
        return color(msg, '&');
    }

    @NotNull
    public static String[] color(String... msgs) {
        String[] res = new String[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            res[i] = color(msgs[i]);
        }
        return res;
    }

    @NotNull
    public static String color(@Nullable String msg, char colorChar) {
        return msg == null ? "" : ChatColor.translateAlternateColorCodes(colorChar, msg);
    }
}
