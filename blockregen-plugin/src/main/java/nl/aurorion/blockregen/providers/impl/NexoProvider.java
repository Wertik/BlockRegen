package nl.aurorion.blockregen.providers.impl;

import com.nexomc.nexo.api.NexoBlocks;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.CompatibilityProvider;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import nl.aurorion.blockregen.system.material.NexoMaterial;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class NexoProvider extends CompatibilityProvider implements MaterialParser {
    public NexoProvider(BlockRegen plugin) {
        super(plugin, "nexo");
        setFeatures("materials");
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!NexoBlocks.isCustomBlock(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not a Nexo block.", input));
        }
        return new NexoMaterial(this.plugin, input);
    }
}
