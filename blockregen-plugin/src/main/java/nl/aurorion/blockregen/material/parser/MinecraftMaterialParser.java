package nl.aurorion.blockregen.material.parser;

import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MinecraftMaterial;
import nl.aurorion.blockregen.util.Parsing;
import nl.aurorion.blockregen.version.api.NodeData;
import org.jetbrains.annotations.NotNull;

@Log
public class MinecraftMaterialParser implements MaterialParser {

    private final BlockRegenPlugin plugin;

    public MinecraftMaterialParser(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) {
        log.fine(() -> String.format("Parsing MC material from %s", input));

        boolean loadData = false;

        String materialPart = input;
        if (input.contains("[")) {
            materialPart = input.substring(0, input.indexOf("["));
            loadData = true;
        }

        // On 1.12.2 and below, wheat is not considered a block. Just allow using any materials.
        XMaterial xMaterial = Parsing.parseMaterial(materialPart, plugin.getVersionManager().isCurrentAbove("1.12.2", false));

        if (loadData) {
            NodeData nodeData = plugin.getVersionManager().getNodeDataParser().parse(String.format("minecraft:%s", input));
            return new MinecraftMaterial(plugin, xMaterial, nodeData);
        } else {
            return new MinecraftMaterial(plugin, xMaterial);
        }
    }
}
