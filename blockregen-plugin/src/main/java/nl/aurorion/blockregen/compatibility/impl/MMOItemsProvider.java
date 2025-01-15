package nl.aurorion.blockregen.compatibility.impl;

import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class MMOItemsProvider extends CompatibilityProvider implements MaterialParser {
    public MMOItemsProvider(BlockRegenPlugin plugin) {
        super(plugin, "mmoitems");
        setFeatures("materials");
    }

    /**
     * @throws ParseException If parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) {
        try {
            int id = Integer.parseInt(input);
            return new MMOIItemsMaterial(plugin, id);
        } catch (NumberFormatException e) {
            throw new ParseException(String.format("Invalid MMOItem block id: '%s'.", input));
        }
    }
}
