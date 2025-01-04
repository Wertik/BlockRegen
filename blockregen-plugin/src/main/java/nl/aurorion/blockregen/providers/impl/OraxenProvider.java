package nl.aurorion.blockregen.providers.impl;

import io.th0rgal.oraxen.api.OraxenBlocks;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.CompatibilityProvider;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import nl.aurorion.blockregen.system.material.OraxenMaterial;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class OraxenProvider extends CompatibilityProvider implements MaterialParser {

    public OraxenProvider(BlockRegen plugin) {
        super(plugin, "oraxen");
        setFeatures("materials");
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!OraxenBlocks.isOraxenBlock(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not an Oraxen block.", input));
        }

        return new OraxenMaterial(this.plugin, input);
    }
}
