package nl.aurorion.blockregen.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Serialization {
    @NotNull
    public static String serializeNodeDataEntries(@NotNull Map<String, Object> entries) {
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
