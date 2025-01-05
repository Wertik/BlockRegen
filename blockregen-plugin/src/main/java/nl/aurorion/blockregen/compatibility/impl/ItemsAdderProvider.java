package nl.aurorion.blockregen.compatibility.impl;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.ItemsAdderMaterial;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemsAdderProvider extends CompatibilityProvider implements MaterialParser, ItemProvider {
    public ItemsAdderProvider(BlockRegen plugin) {
        super(plugin, "ia");
        setFeatures("materials", "drops");
    }

    @Override
    public @NotNull BlockRegenMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!CustomBlock.isInRegistry(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid ItemsAdder custom block.", input));
        }

        return new ItemsAdderMaterial(input);
    }

    @Override
    public ItemStack createItem(String id, Function<String, String> parser, int amount) {
        CustomStack builder = CustomStack.getInstance(id);
        builder.setDisplayName(parser.apply(builder.getDisplayName()));
        ItemStack item = builder.getItemStack();
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        if (meta.getLore() != null) {
            meta.setLore(meta.getLore().stream()
                    .map(parser)
                    .collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean exists(String id) {
        return CustomStack.isInRegistry(id);
    }

    @Override
    public boolean containsColon() {
        return true;
    }
}
