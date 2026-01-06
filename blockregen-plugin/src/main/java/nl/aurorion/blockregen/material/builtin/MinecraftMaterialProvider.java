package nl.aurorion.blockregen.material.builtin;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import nl.aurorion.blockregen.util.BukkitVersions;
import nl.aurorion.blockregen.util.Locations;
import nl.aurorion.blockregen.util.Parsing;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

@Log
public class MinecraftMaterialProvider implements MaterialProvider {

    private final BlockRegenPlugin plugin;

    public MinecraftMaterialProvider(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(@NotNull String input) {
        log.fine(() -> String.format("Parsing MC material from '%s'", input));

        boolean loadData = false;

        String materialPart = input;
        if (input.contains("[")) {
            materialPart = input.substring(0, input.indexOf("["));
            loadData = true;
        }

        // On 1.12.2 and below, wheat is not considered a block. Just allow using any materials.
        XMaterial xMaterial = Parsing.parseMaterial(materialPart, BukkitVersions.isCurrentAbove("1.12.2", false));

        if (loadData) {
            NodeData nodeData = plugin.getVersionManager().getNodeDataParser().parse(String.format("minecraft:%s", input));
            return new MinecraftMaterial(plugin, xMaterial, nodeData);
        } else {
            return new MinecraftMaterial(plugin, xMaterial);
        }
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NotNull Block block) {
        log.fine(() -> "Loading MC material from block " + Locations.locationToString(block.getLocation()));

        XMaterial material = plugin.getVersionManager().getMethods().getType(block);

        NodeData nodeData = plugin.getVersionManager().createNodeData();
        nodeData.load(block);

        return new MinecraftMaterial(plugin, material, nodeData);
    }

    @Override
    public BlockRegenMaterial createInstance(Type type) {
        log.fine(() -> "Created empty MinecraftMaterial.");
        return new MinecraftMaterial(plugin, null, null);
    }

    @Override
    public @NotNull Class<?> getClazz() {
        return MinecraftMaterial.class;
    }
}
