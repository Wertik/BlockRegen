package nl.aurorion.blockregen.material;

import com.google.gson.InstanceCreator;
import nl.aurorion.blockregen.ParseException;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MaterialProvider extends InstanceCreator<BlockRegenMaterial> {

    /**
     * Parse a BlockRegenMaterial from an input string.
     *
     * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input =
     *              'caveblock').
     * @return Parsed BlockRegenMaterial
     * @throws ParseException if the provided {@code input} is not a valid oraxen block id
     */
    @NotNull
    BlockRegenMaterial parseMaterial(@NotNull String input);

    // Return null if the block isn't recognized by this loader.
    @Nullable
    BlockRegenMaterial load(@NotNull Block block);

    /**
     * @return True if the material syntax contains colons.
     */
    default boolean containsColon() {
        return false;
    }

    @NotNull
    Class<?> getClazz();
}
