package nl.aurorion.blockregen;

import com.google.gson.GsonBuilder;
import com.linecorp.conditional.Condition;
import com.linecorp.conditional.ConditionContext;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.api.version.VersionManager;
import nl.aurorion.blockregen.command.Commands;
import nl.aurorion.blockregen.compatibility.CompatibilityManager;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.configuration.ParseException;
import nl.aurorion.blockregen.drop.ItemManager;
import nl.aurorion.blockregen.event.EventManager;
import nl.aurorion.blockregen.listener.PlayerListener;
import nl.aurorion.blockregen.listener.RegenerationListener;
import nl.aurorion.blockregen.material.MaterialManager;
import nl.aurorion.blockregen.material.parser.MinecraftMaterialParser;
import nl.aurorion.blockregen.particle.ParticleManager;
import nl.aurorion.blockregen.particle.impl.FireWorks;
import nl.aurorion.blockregen.particle.impl.FlameCrown;
import nl.aurorion.blockregen.particle.impl.WitchSpell;
import nl.aurorion.blockregen.preset.PresetManager;
import nl.aurorion.blockregen.preset.condition.GenericConditionProvider;
import nl.aurorion.blockregen.regeneration.RegenerationManager;
import nl.aurorion.blockregen.region.RegionManager;
import nl.aurorion.blockregen.version.NodeDataAdapter;
import nl.aurorion.blockregen.version.NodeDataInstanceCreator;
import nl.aurorion.blockregen.version.VersionManagerImpl;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Log
public class BlockRegenPluginImpl extends JavaPlugin implements Listener, BlockRegenPlugin {

    private static final String PACKAGE_NAME = BlockRegenPluginImpl.class.getPackage().getName();

    private static BlockRegenPlugin instance;

    public static BlockRegenPlugin getInstance() {
        return BlockRegenPluginImpl.instance;
    }

    @Getter
    private final Random random = new Random();

    public String newVersion = null;

    @Getter
    private boolean usePlaceholderAPI = false;

    private boolean finishedLoading = false;

    @Getter
    private final VersionManager versionManager = new VersionManagerImpl(this);

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
    private final CompatibilityManager compatibilityManager = new CompatibilityManager(this);

    @Getter
    private GsonHelper gsonHelper;

    @Getter
    private ConsoleHandler consoleHandler;

    @Override
    public void onEnable() {
        BlockRegenPluginImpl.instance = this;

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

        // Default condition parsers


        checkPlaceholderAPI();

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

    @Override
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

    @Override
    public @NotNull Level getLogLevel() {
        return getParentLogger().getLevel();
    }

    @Override
    public void setLogLevel(@NotNull Level level) {
        Logger parentLogger = getParentLogger();
        parentLogger.setLevel(level);
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return files.getSettings().getFileConfiguration();
    }
}