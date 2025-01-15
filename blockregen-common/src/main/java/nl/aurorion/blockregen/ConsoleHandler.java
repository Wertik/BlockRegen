package nl.aurorion.blockregen;

import lombok.Getter;
import nl.aurorion.blockregen.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ConsoleHandler extends Handler {
    private final static String NORMAL_PATTERN = "%s&r%s: %s";

    @Getter
    private final JavaPlugin plugin;

    private final ConsoleCommandSender console;

    @Getter
    private String prefix = "";

    @Getter
    private final Set<CommandSender> listeners = new HashSet<>();

    protected ConsoleHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.console = plugin.getServer().getConsoleSender();
    }

    @Override
    public void publish(LogRecord record) {

        String levelName = String.format("&7%s", record.getLevel().getName());
        if (record.getLevel().intValue() < Level.INFO.intValue()) {
            levelName = "&eDEBUG";
        } else if (record.getLevel().intValue() > Level.INFO.intValue()) {
            levelName = "&c" + record.getLevel().getName();
        }

        // Format the message corresponding to the format.
        String message = String.format(NORMAL_PATTERN, prefix, levelName, record.getMessage());

        sendRaw(record, message);
    }

    private void sendRaw(LogRecord record, String msg) {

        if (com.google.common.base.Strings.isNullOrEmpty(msg)) {
            return;
        }

        String coloredMessage = Strings.color(msg);

        toListeners(coloredMessage);

        if (console == null) {
            // Strip color characters that might've been in the message.
            Bukkit.getLogger().log(record.getLevel(), Strings.stripColor(msg));
        } else {
            console.sendMessage(coloredMessage);
        }
    }

    @Override
    public void flush() {
        // NO-OP
    }

    @Override
    public void close() throws SecurityException {
        // NO-OP
    }

    /**
     * Add a listener.
     * CommandSender will receive console output.
     *
     * @param listener CommandSender to add
     */
    public void addListener(@NotNull CommandSender listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     * CommandSender will not receive console output anymore.
     *
     * @param listener CommandSender to remove
     */
    public void removeListener(@NotNull CommandSender listener) {
        listeners.remove(listener);
    }

    public void toListeners(String message) {
        if (message != null)
            listeners.forEach(c -> c.sendMessage(message));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }
}
