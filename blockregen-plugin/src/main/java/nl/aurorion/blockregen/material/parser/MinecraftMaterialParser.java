package nl.aurorion.blockregen.material.parser;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.util.Parsing;
import nl.aurorion.blockregen.material.MinecraftMaterial;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.version.api.NodeData;
import org.jetbrains.annotations.NotNull;

@Log
public class MinecraftMaterialParser implements MaterialParser {

    private final BlockRegenPlugin plugin;

    public MinecraftMaterialParser(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        log.fine(() -> String.format("Parsing MC material from %s", input));

        boolean loadData = false;

        String materialPart = input;
        if (input.contains("[")) {
            materialPart = input.substring(0, input.indexOf("["));
            loadData = true;
        }

        // On 1.12.2 and below, wheat is not considered a block. Just allow using any materials.
        XMaterial xMaterial = Parsing.parseMaterial(materialPart, plugin.getVersionManager().isCurrentAbove("1.12.2", false));

        if (xMaterial == null) {
            throw new IllegalArgumentException("Could not parse minecraft material '" + materialPart + "'.");
        }

        if (loadData) {
            NodeData nodeData = plugin.getVersionManager().getNodeDataParser().parse(String.format("minecraft:%s", input));
            return new MinecraftMaterial(plugin, xMaterial, nodeData);
        } else {
            return new MinecraftMaterial(plugin, xMaterial);
        }
    }
}
