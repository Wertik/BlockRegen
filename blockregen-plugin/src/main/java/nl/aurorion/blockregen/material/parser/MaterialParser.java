package nl.aurorion.blockregen.material.parser;

import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import org.jetbrains.annotations.NotNull;

public interface MaterialParser {

    /**
     * Parse a BlockRegenMaterial from an input string.
     *
     * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input =
     *              'caveblock').
     * @return Parsed BlockRegenMaterial
     * @throws ParseException if the provided {@code input} is not a valid oraxen block id
     */
    @NotNull
    BlockRegenMaterial parseMaterial(String input);

    /**
     * @return True if the material syntax contains colons.
     */
    default boolean containsColon() {
        return false;
    }
}
