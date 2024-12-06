package nl.aurorion.blockregen.raincloud;

import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals", "unused"})
public class CommandContext<C extends CommandSender> {

    private final C sender;

    private final String label;

    private final String[] args;

    private final Map<String, Object> values = new HashMap<>();

    public CommandContext(C sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args;
    }

    public <V extends @NonNull Object> void add(String key, @NonNull V value) {
        this.values.put(key, value);
    }

    public <V> V get(String key) {
        return (V) this.values.get(key);
    }

    public <V> V getOrDefault(String key, V defaultValue) {
        return (V) this.values.getOrDefault(key, defaultValue);
    }

    public C sender() {
        return this.sender;
    }

    public String[] args() {
        return this.args;
    }

    public String label() {
        return this.label;
    }
}
