package nl.aurorion.blockregen.providers.impl;

import dev.lone.itemsadder.api.CustomBlock;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.CompatibilityProvider;
import nl.aurorion.blockregen.system.material.BlockRegenMaterial;
import nl.aurorion.blockregen.system.material.ItemsAdderMaterial;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import org.jetbrains.annotations.NotNull;

public class ItemsAdderProvider extends CompatibilityProvider implements MaterialParser {
    public ItemsAdderProvider(BlockRegen plugin) {
        super(plugin, "ia");
        setFeatures("materials");
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!CustomBlock.isInRegistry(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid ItemsAdder custom block.", input));
        }

        return new ItemsAdderMaterial(input);
    }

    @Override
    public boolean containsColon() {
        return true;
    }
}
