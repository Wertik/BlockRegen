package nl.aurorion.blockregen.compatibility.impl;

import com.nexomc.nexo.api.NexoBlocks;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.NexoMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class NexoProvider extends CompatibilityProvider implements MaterialParser {
    public NexoProvider(BlockRegenPlugin plugin) {
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
