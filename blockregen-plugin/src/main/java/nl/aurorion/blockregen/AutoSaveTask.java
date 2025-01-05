package nl.aurorion.blockregen;

import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

@Log
public class AutoSaveTask implements Runnable {

    private int period;

    private BukkitTask task;

    @Getter
    private boolean running = false;

    private final BlockRegen plugin;

    public AutoSaveTask(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.period = plugin.getConfig().getInt("Auto-Save.Interval", 300);
    }

    public void start() {
        if (running) {
            stop();
        }

        running = true;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, period * 20L, period * 20L);
        log.info("Starting auto-save.. with an interval of " + period + " seconds.");
    }

    public void stop() {
        if (!running) {
            return;
        }

        if (task == null) {
            running = false;
            return;
        }

        task.cancel();
        task = null;
        running = false;
    }

    @Override
    public void run() {
        plugin.getRegenerationManager().save();
        plugin.getRegionManager().save();
    }
}