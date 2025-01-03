package nl.aurorion.blockregen.system.material.parser;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import org.jetbrains.annotations.NotNull;

public class MMOItemsMaterialParser implements MaterialParser {

    private final BlockRegen plugin;

    public MMOItemsMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
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
