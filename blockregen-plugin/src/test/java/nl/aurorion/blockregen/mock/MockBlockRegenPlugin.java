package nl.aurorion.blockregen.mock;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import nl.aurorion.blockregen.ConsoleHandler;
import nl.aurorion.blockregen.GsonHelper;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.api.version.VersionManager;
import nl.aurorion.blockregen.compatibility.CompatibilityManager;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.drop.ItemManager;
import nl.aurorion.blockregen.event.EventManager;
import nl.aurorion.blockregen.material.MaterialManager;
import nl.aurorion.blockregen.particle.ParticleManager;
import nl.aurorion.blockregen.preset.PresetManager;
import nl.aurorion.blockregen.regeneration.RegenerationManager;
import nl.aurorion.blockregen.region.RegionManager;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockBlockRegenPlugin implements BlockRegenPlugin {

    @Getter
    private final VersionManager versionManager = new MockVersionManager();

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

    @Override
    public void reload(CommandSender sender) {

    }

    @Override
    public @NotNull Level getLogLevel() {
        return null;
    }

    @Override
    public void setLogLevel(@NotNull Level level) {

    }

    @Override
    public @NotNull File getDataFolder() {
        return null;
    }

    @Override
    public @NotNull PluginDescriptionFile getDescription() {
        return null;
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return null;
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String s) {
        return null;
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(@NotNull String s, boolean b) {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public @NotNull PluginLoader getPluginLoader() {
        return null;
    }

    @Override
    public @NotNull Server getServer() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(boolean b) {

    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String s, @Nullable String s1) {
        return null;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull String s, @Nullable String s1) {
        return null;
    }

    @Override
    public @NotNull Logger getLogger() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return "";
    }

    @Override
    public Random getRandom() {
        return null;
    }

    @Override
    public boolean isUsePlaceholderAPI() {
        return false;
    }

    @Override
    public GsonHelper getGsonHelper() {
        return null;
    }

    @Override
    public ConsoleHandler getConsoleHandler() {
        return null;
    }

    @Override
    public XMaterial getBlockType(Block block) {
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return new ArrayList<>();
    }
}
