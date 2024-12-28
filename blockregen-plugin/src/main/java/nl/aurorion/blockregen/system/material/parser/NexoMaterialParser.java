package nl.aurorion.blockregen.system.material.parser;

import com.nexomc.nexo.api.NexoBlocks;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.material.NexoMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import org.jetbrains.annotations.NotNull;

public class NexoMaterialParser implements MaterialParser {

    private final BlockRegen plugin;

    public NexoMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull TargetMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!NexoBlocks.isCustomBlock(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not a Nexo block.", input));
        }
        return new NexoMaterial(this.plugin, input);
    }
}
