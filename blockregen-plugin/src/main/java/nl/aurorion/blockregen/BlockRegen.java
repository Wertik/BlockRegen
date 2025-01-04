package nl.aurorion.blockregen;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.commands.Commands;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.listeners.PlayerListener;
import nl.aurorion.blockregen.listeners.RegenerationListener;
import nl.aurorion.blockregen.particles.ParticleManager;
import nl.aurorion.blockregen.particles.impl.FireWorks;
import nl.aurorion.blockregen.particles.impl.FlameCrown;
import nl.aurorion.blockregen.particles.impl.WitchSpell;
import nl.aurorion.blockregen.providers.CompatibilityManager;
import nl.aurorion.blockregen.system.GsonHelper;
import nl.aurorion.blockregen.system.drop.ItemManager;
import nl.aurorion.blockregen.system.event.EventManager;
import nl.aurorion.blockregen.system.material.MaterialManager;
import nl.aurorion.blockregen.system.material.parser.MinecraftMaterialParser;
import nl.aurorion.blockregen.system.preset.PresetManager;
import nl.aurorion.blockregen.system.regeneration.RegenerationManager;
import nl.aurorion.blockregen.system.region.RegionManager;
import nl.aurorion.blockregen.version.NodeDataAdapter;
import nl.aurorion.blockregen.version.NodeDataInstanceCreator;
import nl.aurorion.blockregen.version.VersionManager;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Log
public class BlockRegen extends JavaPlugin implements Listener {

    private static final String PACKAGE_NAME = BlockRegen.class.getPackage().getName();

    private static BlockRegen instance;

    public static BlockRegen getInstance() {
        return BlockRegen.instance;
    }

    @Getter
    private final Random random = new Random();

    public String newVersion = null;

    @Getter
    private boolean usePlaceholderAPI = false;

    private boolean finishedLoading = false;

    @Getter
    private final VersionManager versionManager = new VersionManager(this);

    @Getter
    private final Files files = new Files(this);

    @Getter
    private final PresetManager presetManager = new PresetManager(this);

    @Getter
    private final ParticleManager particleManager = new ParticleManager();

    @Getter
    private final RegenerationManager regenerationManager = new RegenerationManager(this);

    @Getter
    private final RegionManager regionManager = new RegionManager(this);

    @Getter
    private final EventManager eventManager = new EventManager(this);

    @Getter
    private final MaterialManager materialManager = new MaterialManager(this);

    @Getter
    private final ItemManager itemManager = new ItemManager(this);

    @Getter
    private GsonHelper gsonHelper;

    @Getter
    private ConsoleHandler consoleHandler;

    @Getter
    private CompatibilityManager compatibilityManager;

    @Override
    public void onEnable() {
        BlockRegen.instance = this;

        setupLogger();
        files.load();
        configureLogger();

        log.info("Running on version " + versionManager.getVersion());

        versionManager.load();

        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeHierarchyAdapter(NodeData.class, new NodeDataAdapter<>())
                .registerTypeAdapter(NodeData.class, new NodeDataInstanceCreator(versionManager.getNodeProvider()))
                .setPrettyPrinting();
        gsonHelper = new GsonHelper(gsonBuilder);

        // Add default particles
        new FireWorks().register();
        new FlameCrown().register();
        new WitchSpell().register();

        Message.load();

        // Default material parsers for minecraft materials
        materialManager.registerParser(null, new MinecraftMaterialParser(this));
        materialManager.registerParser("minecraft", new MinecraftMaterialParser(this));

        checkPlaceholderAPI();

        this.compatibilityManager = new CompatibilityManager(this);
        compatibilityManager.discover(false);

        presetManager.load();
        regionManager.load();
        regenerationManager.load();

        finishedLoading = true;

        registerListeners();

        Objects.requireNonNull(getCommand("blockregen")).setExecutor(new Commands(this));

        String ver = getDescription().getVersion();

        log.info("&bYou are using" + (ver.contains("-SNAPSHOT") || ver.contains("-b") ? " &cDEVELOPMENT&b" : "")
                + " version &f" + getDescription().getVersion());
        log.info("&bReport bugs or suggestions to discord only please. &f( /blockregen discord )");
        log.info("&bAlways backup if you are not sure about things.");

        enableMetrics();

        if (getConfig().getBoolean("Update-Checker", false)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                UpdateCheck updater = new UpdateCheck(this, 9885);
                try {
                    if (updater.checkForUpdates()) {
                        newVersion = updater.getLatestVersion();
                    }
                } catch (Exception e) {
                    log.warning("Could not check for updates.");
                }
            }, 20L);
        }

        // Check for deps and start auto save once the server is done loading.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            compatibilityManager.discover(true);

            if (getConfig().getBoolean("Auto-Save.Enabled", false)) {
                regenerationManager.startAutoSave();
            }

            regenerationManager.reattemptLoad();
            regionManager.reattemptLoad();
        }, 1L);
    }

    public void reload(CommandSender sender) {

        if (!(sender instanceof ConsoleCommandSender))
            this.consoleHandler.addListener(sender);

        eventManager.disableAll();
        eventManager.clearBars();

        versionManager.load();

        compatibilityManager.discover(false);
        checkPlaceholderAPI();

        files.getSettings().load();

        configureLogger();

        files.getMessages().load();
        Message.load();

        files.getBlockList().load();
        presetManager.load();

        regionManager.reload();

        if (getConfig().getBoolean("Auto-Save.Enabled", false))
            regenerationManager.reloadAutoSave();

        this.consoleHandler.removeListener(sender);
        sender.sendMessage(Message.RELOAD.get());
    }

    @Override
    public void onDisable() {
        if (regenerationManager.getAutoSaveTask() != null) {
            regenerationManager.getAutoSaveTask().stop();
        }

        if (finishedLoading) {
            regenerationManager.revertAll();
            regenerationManager.save(true);

            regionManager.save();
        }

        this.teardownLogger();
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new RegenerationListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    public void checkDependencies(boolean reloadPresets) {
        log.info("Checking dependencies...");

    }

    private void checkPlaceholderAPI() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && !usePlaceholderAPI) {
            usePlaceholderAPI = true;
            log.info("Found PlaceholderAPI! &aUsing it for placeholders.");
        }
    }

    public void enableMetrics() {
        new MetricsLite(this);
        log.info("&8MetricsLite enabled");
    }

    private static Logger getParentLogger() {
        return Logger.getLogger(PACKAGE_NAME);
    }

    private void setupLogger() {
        // Add the handler only to the parent logger of our plugin package.
        Logger parentLogger = getParentLogger();

        this.consoleHandler = new ConsoleHandler(this);

        parentLogger.setUseParentHandlers(false); // Disable default bukkit logger for us.
        parentLogger.addHandler(this.consoleHandler);
    }

    private void configureLogger() {
        this.consoleHandler.setPrefix(Message.PREFIX.getValue());

        boolean debug = files.getSettings().getFileConfiguration().getBoolean("Debug-Enabled", false);

        setLogLevel(debug ? Level.FINE : Level.INFO);
    }

    private void teardownLogger() {
        Logger parentLogger = getParentLogger();

        parentLogger.removeHandler(this.consoleHandler);
        parentLogger.setLevel(Level.INFO);

        this.consoleHandler = null;
    }

    public Level getLogLevel() {
        return getParentLogger().getLevel();
    }

    public void setLogLevel(Level level) {
        Logger parentLogger = getParentLogger();
        parentLogger.setLevel(level);
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return files.getSettings().getFileConfiguration();
    }
}