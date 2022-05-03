package nl.aurorion.blockregen;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import lombok.Getter;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import nl.aurorion.blockregen.commands.Commands;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.listeners.BlockListener;
import nl.aurorion.blockregen.listeners.PlayerListener;
import nl.aurorion.blockregen.particles.ParticleManager;
import nl.aurorion.blockregen.particles.impl.FireWorks;
import nl.aurorion.blockregen.particles.impl.FlameCrown;
import nl.aurorion.blockregen.particles.impl.WitchSpell;
import nl.aurorion.blockregen.providers.JobsProvider;
import nl.aurorion.blockregen.system.GsonHelper;
import nl.aurorion.blockregen.system.event.EventManager;
import nl.aurorion.blockregen.system.preset.PresetManager;
import nl.aurorion.blockregen.system.regeneration.RegenerationManager;
import nl.aurorion.blockregen.system.region.RegionManager;
import nl.aurorion.blockregen.version.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BlockRegen extends JavaPlugin {

    public static BlockRegen getInstance() {
        return getPlugin(BlockRegen.class);
    }

    // Dependencies
    @Getter
    private Economy economy;
    @Getter
    private GriefPrevention griefPrevention;
    @Getter
    private ResidenceApi residence;
    @Getter
    private JobsProvider jobsProvider;

    @Getter
    private VersionManager versionManager;

    @Getter
    private boolean usePlaceholderAPI = false;

    @Getter
    private Files files;

    @Getter
    private Random random;

    @Getter
    public ConsoleOutput consoleOutput;

    public String newVersion = null;

    @Getter
    private PresetManager presetManager;

    @Getter
    private ParticleManager particleManager;

    @Getter
    private RegenerationManager regenerationManager;

    @Getter
    private RegionManager regionManager;

    @Getter
    private EventManager eventManager;

    @Getter
    private GsonHelper gsonHelper;

    @Override
    public void onEnable() {
        random = new Random();

        consoleOutput = ConsoleOutput.getInstance(this);

        files = new Files(this);

        consoleOutput.setDebug(files.getSettings().getFileConfiguration().getBoolean("Debug-Enabled", false));
        consoleOutput.setPrefix(StringUtil.color(Message.PREFIX.getValue()));

        versionManager = new VersionManager(this);
        consoleOutput.info("Running on version " + versionManager.getVersion());

        versionManager.load();

        gsonHelper = new GsonHelper();

        particleManager = new ParticleManager();

        // Add default particles
        new FireWorks().register();
        new FlameCrown().register();
        new WitchSpell().register();

        presetManager = new PresetManager(this);
        regenerationManager = new RegenerationManager(this);
        regionManager = new RegionManager(this);
        eventManager = new EventManager(this);

        Message.load();

        checkDependencies(false);

        presetManager.loadAll();
        regionManager.load();
        regenerationManager.load();

        registerListeners();

        getCommand("blockregen").setExecutor(new Commands(this));

        String ver = getDescription().getVersion();

        consoleOutput.info("&bYou are using" + (ver.contains("-SNAPSHOT") || ver.contains("-b") ? " &cDEVELOPMENT&b" : "") + " version &f" + getDescription().getVersion());
        consoleOutput.info("&bReport bugs or suggestions to discord only please. &f( /blockregen discord )");
        consoleOutput.info("&bAlways backup if you are not sure about things.");

        enableMetrics();
        if (getConfig().getBoolean("Update-Checker", false)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                UpdateCheck updater = new UpdateCheck(this, 9885);
                try {
                    if (updater.checkForUpdates()) {
                        newVersion = updater.getLatestVersion();
                    }
                } catch (Exception e) {
                    consoleOutput.warn("Could not check for updates.");
                }
            }, 20L);
        }

        // Check for deps and start auto save once the server is done loading.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            checkDependencies(true);

            if (getConfig().getBoolean("Auto-Save.Enabled", false))
                regenerationManager.startAutoSave();

            regenerationManager.reattemptLoad();
            regionManager.reattemptLoad();
        }, 1L);
    }

    public void reload(CommandSender sender) {

        if (!(sender instanceof ConsoleCommandSender))
            consoleOutput.addListener(sender);

        eventManager.disableAll();
        eventManager.clearBars();

        // Load again in case something got installed.
        versionManager.load();

        checkDependencies(false);

        files.getSettings().load();
        consoleOutput.setDebug(files.getSettings().getFileConfiguration().getBoolean("Debug-Enabled", false));

        files.getMessages().load();
        Message.load();

        consoleOutput.setPrefix(StringUtil.color(Message.PREFIX.getValue()));
        consoleOutput.setDebug(getConfig().getBoolean("Debug-Enabled", false));

        files.getBlockList().load();
        presetManager.loadAll();

        if (getConfig().getBoolean("Auto-Save.Enabled", false))
            regenerationManager.reloadAutoSave();

        consoleOutput.removeListener(sender);
        sender.sendMessage(Message.RELOAD.get());
    }

    @Override
    public void onDisable() {
        if (regenerationManager.getAutoSaveTask() != null)
            regenerationManager.getAutoSaveTask().stop();

        regenerationManager.revertAllonMain();
        regenerationManager.save();

        regionManager.save();
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new BlockListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    public void checkDependencies(boolean reloadPresets) {
        consoleOutput.info("Checking dependencies...");
        setupEconomy();
        setupJobs(reloadPresets);
        setupResidence();
        setupGriefPrevention();
        setupPlaceholderAPI();
    }

    private void setupEconomy() {
        if (economy != null) return;

        if (!getServer().getPluginManager().isPluginEnabled("Vault"))
            return;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            consoleOutput.info("Found Vault, but no Economy Provider is registered.");
            return;
        }

        economy = rsp.getProvider();
        consoleOutput.info("Vault & economy plugin found! &aEnabling economy functions.");
    }

    private void setupJobs(boolean reloadPresets) {
        if (getServer().getPluginManager().isPluginEnabled("Jobs") && jobsProvider == null) {
            this.jobsProvider = new JobsProvider();
            consoleOutput.info("Jobs found! &aEnabling Jobs requirements and rewards.");

            if (reloadPresets) {
                // Load presets again because of jobs check.
                this.presetManager.loadAll();
                consoleOutput.info("Reloading presets to add jobs requirements...");
            }
        }
    }

    private void setupGriefPrevention() {
        if (getServer().getPluginManager().isPluginEnabled("GriefPrevention") && griefPrevention == null) {
            this.griefPrevention = GriefPrevention.instance;
            consoleOutput.info("GriefPrevention found! &aSupporting it's protection.");
        }
    }

    private void setupResidence() {
        if (getServer().getPluginManager().isPluginEnabled("Residence") && residence == null) {
            this.residence = Residence.getInstance().getAPI();
            consoleOutput.info("Found Residence! &aRespecting it's protection.");
        }
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && !usePlaceholderAPI) {
            usePlaceholderAPI = true;
            consoleOutput.info("Found PlaceholderAPI! &aUsing it for placeholders.");
        }
    }

    public void enableMetrics() {
        new MetricsLite(this);
        consoleOutput.info("&8MetricsLite enabled");
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return files.getSettings().getFileConfiguration();
    }
}