package nl.aurorion.blockregen.providers.impl;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.CompatibilityProvider;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import nl.aurorion.blockregen.system.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class MMOItemsProvider extends CompatibilityProvider implements MaterialParser {
    public MMOItemsProvider(BlockRegen plugin) {
        super(plugin, "mmoitems");
        setFeatures("materials");
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        try {
            int id = Integer.parseInt(input);
            return new MMOIItemsMaterial(plugin, id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid MMOItem block id: '%s'.", input));
        }
    }
}
