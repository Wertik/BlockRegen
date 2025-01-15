package nl.aurorion.blockregen.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@UtilityClass
public class Strings {
    public String stripColor(String msg) {
        return msg != null ? ChatColor.stripColor(msg) : null;
    }

    @NotNull
    public String color(@Nullable String msg) {
        return color(msg, '&');
    }

    @NotNull
    public String[] color(String... msgs) {
        String[] res = new String[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            res[i] = color(msgs[i]);
        }
        return res;
    }

    @NotNull
    public String color(@Nullable String msg, char colorChar) {
        return msg == null ? "" : ChatColor.translateAlternateColorCodes(colorChar, msg);
    }

    public String serializeNodeDataEntries(Map<String, Object> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (entry.getValue() == null) continue;
            builder.append(String.format("%s=%s,", entry.getKey(), entry.getValue()));
        }
        int lastComma = builder.lastIndexOf(",");
        if (lastComma != -1) {
            builder.deleteCharAt(lastComma);
        }
        builder.append("]");
        return builder.toString();
    }
}
