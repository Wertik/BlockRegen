package nl.Aurorion.BlockRegen;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import nl.Aurorion.BlockRegen.Commands.Commands;
import nl.Aurorion.BlockRegen.Configurations.Files;
import nl.Aurorion.BlockRegen.Events.BlockBreak;
import nl.Aurorion.BlockRegen.Events.PlayerInteract;
import nl.Aurorion.BlockRegen.Events.PlayerJoin;
import nl.Aurorion.BlockRegen.Particles.ParticleUtil;
import nl.Aurorion.BlockRegen.System.ConsoleOutput;
import nl.Aurorion.BlockRegen.System.ExceptionHandler;
import nl.Aurorion.BlockRegen.System.Getters;
import nl.Aurorion.BlockRegen.System.UpdateCheck;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class Main extends JavaPlugin {

    public Main plugin;
    public Economy econ;
    public WorldEditPlugin worldEdit;
    public GriefPrevention griefPrevention;

    private Files files;
    private Messages messages;
    private ParticleUtil particleUtil;
    private Getters getters;
    private Random random;

    // Handles every output going to console, easier, more centralized control.
    public ConsoleOutput cO;
    // Handles exceptions, pretties them and prints info along with them.
    public ExceptionHandler eH;

    public String newVersion = null;

    @Override
    public void onEnable() {
        plugin = this;
        this.registerClasses(); // Also generates files
        cO = new ConsoleOutput(this);
        cO.setDebug(files.settings.getBoolean("Debug-Enabled", false));
        cO.setPrefix(ChatColor.translateAlternateColorCodes('&', files.messages.getString("Messages.Prefix")));
        eH = new ExceptionHandler(this);
        this.registerCommands();
        this.registerEvents();
        this.fillEvents();
        this.setupEconomy();
        this.setupWorldEdit();
        this.checkForPlugins();
        Utils.fillFireworkColors();
        this.recoveryCheck();
        cO.info("&bYou are using version " + this.getDescription().getVersion());
        cO.info("&bReport bugs or suggestions to discord only please.");
        cO.info("&bAlways backup if you are not sure about things.");
        this.enableMetrics();
        if (this.getGetters().updateChecker()) {
            this.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                UpdateCheck updater = new UpdateCheck(this, 9885);
                try {
                    if (updater.checkForUpdates()) {
                        this.newVersion = updater.getLatestVersion();
                    }
                } catch (Exception e) {
                    eH.handleException(e, "Could not check for updates!");
                }
            }, 20L);
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        if (!this.getGetters().dataRecovery() && !Utils.regenBlocks.isEmpty()) {
            for (Location loc : Utils.persist.keySet()) {
                loc.getBlock().setType(Utils.persist.get(loc));
            }
        }
        plugin = null;
    }

    private void registerClasses() {
        files = new Files(this);
        messages = new Messages(files);
        particleUtil = new ParticleUtil(this);
        getters = new Getters(this);
        random = new Random();
    }

    private void registerCommands() {
        this.getCommand("blockregen").setExecutor(new Commands(this));
    }

    private void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new Commands(this), this);
        pm.registerEvents(new BlockBreak(this), this);
        pm.registerEvents(new PlayerInteract(this), this);
        pm.registerEvents(new PlayerJoin(this), this);
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            cO.info("&eDidn't found Vault. &cEconomy functions disabled.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            cO.info("&eVault found, but no economy plugin. &cEconomy functions disabled.");
            return false;
        }
        econ = rsp.getProvider();
        cO.info("&eVault & economy plugin found! &aEnabling economy functions.");
        return econ != null;
    }

    private boolean setupWorldEdit() {
        Plugin worldeditplugin = this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldeditplugin == null || !(worldeditplugin instanceof WorldEditPlugin)) {
            cO.info("&eDidn't found WorldEdit. &cRegion functions disabled.");
            return false;
        }
        cO.info("&eWorldEdit found! &aEnabling region fuctions.");
        worldEdit = (WorldEditPlugin) worldeditplugin;
        return worldEdit != null;
    }

    private void checkForPlugins() {
        if (this.getJobs())
            cO.info("&eJobs found! &aEnabling Jobs fuctions.");
    }

    public void fillEvents() {
        FileConfiguration blocklist = files.getBlocklist();
        ConfigurationSection blocks = blocklist.getConfigurationSection("Blocks");
        Set<String> setblocks = blocks.getKeys(false);
        for (String loopBlocks : setblocks) {
            String eventName = blocklist.getString("Blocks." + loopBlocks + ".event.event-name");
            if (eventName == null) {
                continue;
            } else {
                Utils.events.put(eventName, false);
            }
        }
        if (Utils.events.isEmpty()) {
            this.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&3BlockRegen&6] &cThere are 0 events found. Skip adding to the system."));
        } else {
            this.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&3BlockRegen&6] &aThere are " + Utils.events.keySet().size() + " events found. Added all to the system."));
        }
    }

    public void enableMetrics() {
        new MetricsLite(this);
        this.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&3BlockRegen&6] &8MetricsLite enabled"));
    }

    //-------------------- Getters --------------------------
    public Economy getEconomy() {
        return this.econ;
    }

    public WorldEditPlugin getWorldEdit() {
        return this.worldEdit;
    }

    public boolean getJobs() {
        if (this.getServer().getPluginManager().getPlugin("Jobs") != null) {
            return true;
        }
        return false;
    }

    public GriefPrevention getGriefPrevention() {
        return griefPrevention;
    }

    public Files getFiles() {
        return this.files;
    }

    public Messages getMessages() {
        return this.messages;
    }

    public ParticleUtil getParticles() {
        return this.particleUtil;
    }

    public Getters getGetters() {
        return this.getters;
    }

    public Random getRandom() {
        return this.random;
    }

    public void recoveryCheck() {
        if (this.getGetters().dataRecovery()) {
            Set<String> set = files.getData().getKeys(false);
            if (!set.isEmpty()) {
                while (set.iterator().hasNext()) {
                    String name = set.iterator().next();
                    List<String> list = files.getData().getStringList(name);
                    for (int i = 0; i < list.size(); i++) {
                        Location loc = Utils.stringToLocation(list.get(i));
                        loc.getBlock().setType(Material.valueOf(name));
                        cO.debug("Recovered " + name + " on position " + Utils.locationToString(loc));
                    }
                    set.remove(name);
                }
            }
            for (String key : files.getData().getKeys(false)) {
                files.getData().set(key, null);
            }
            files.saveData();
        }
    }
}
