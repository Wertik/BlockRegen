package nl.aurorion.blockregen;

import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.compatibility.CompatibilityManager;
import nl.aurorion.blockregen.configuration.Files;
import nl.aurorion.blockregen.drop.ItemManager;
import nl.aurorion.blockregen.event.EventManager;
import nl.aurorion.blockregen.material.MaterialManager;
import nl.aurorion.blockregen.particle.ParticleManager;
import nl.aurorion.blockregen.preset.PresetManager;
import nl.aurorion.blockregen.regeneration.RegenerationManager;
import nl.aurorion.blockregen.region.RegionManager;
import nl.aurorion.blockregen.version.VersionManager;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.logging.Level;

public interface BlockRegenPlugin extends Plugin {

    static BlockRegenPlugin getInstance() {
        return BlockRegenPluginImpl.getInstance();
    }

    void reload(CommandSender sender);

    @NotNull Level getLogLevel();

    void setLogLevel(@NotNull Level level);

    @Override
    @NotNull FileConfiguration getConfig();

    Random getRandom();

    boolean isUsePlaceholderAPI();

    VersionManager getVersionManager();

    @NotNull Files getFiles();

    @NotNull PresetManager getPresetManager();

    @NotNull ParticleManager getParticleManager();

    @NotNull RegenerationManager getRegenerationManager();

    @NotNull RegionManager getRegionManager();

    @NotNull EventManager getEventManager();

    @NotNull MaterialManager getMaterialManager();

    @NotNull ItemManager getItemManager();

    GsonHelper getGsonHelper();

    ConsoleHandler getConsoleHandler();

    XMaterial getBlockType(Block block);

    @NotNull CompatibilityManager getCompatibilityManager();
}
