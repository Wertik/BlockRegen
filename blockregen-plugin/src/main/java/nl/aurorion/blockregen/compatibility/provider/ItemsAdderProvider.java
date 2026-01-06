package nl.aurorion.blockregen.compatibility.provider;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.compatibility.material.ItemsAdderMaterial;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.material.MaterialProvider;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log
public class ItemsAdderProvider extends CompatibilityProvider implements ItemProvider, MaterialProvider {
    public ItemsAdderProvider(BlockRegenPlugin plugin) {
        super(plugin, "ia");
        setFeatures("materials", "drops", "conditions");
    }

    @Override
    public void onLoad() {
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/tool", (key, node) -> {
            String id = (String) node;

            if (CustomStack.getInstance(id) == null) {
                throw new ParseException("Invalid ItemsAdder item '" + id + "'.", true);
            }

            return Condition.of((ctx) -> {
                ItemStack tool = (ItemStack) ctx.mustVar("tool");
                CustomStack toolBuilder = CustomStack.byItemStack(tool);
                return toolBuilder != null && toolBuilder.getNamespacedID().equalsIgnoreCase(id);
            });
        });
    }

    /**
     * @throws ParseException If parsing fails.
     */
    @Override
    public @NotNull BlockRegenMaterial parseMaterial(@NotNull String input) {
        if (!CustomBlock.isInRegistry(input)) {
            throw new ParseException(String.format("'%s' is not a valid ItemsAdder custom block.", input), true);
        }
        return new ItemsAdderMaterial(input);
    }

    @Override
    public @Nullable BlockRegenMaterial load(@NotNull Block block) {
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        return customBlock == null ? null : new ItemsAdderMaterial(customBlock.getNamespacedID());
    }

    @Override
    public ItemStack createItem(@NotNull String id, @NotNull Function<String, String> parser, int amount) {
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
    public boolean exists(@NotNull String id) {
        return CustomStack.isInRegistry(id);
    }

    @Override
    public boolean containsColon() {
        return true;
    }

    @Override
    public @NotNull Class<?> getClazz() {
        return ItemsAdderMaterial.class;
    }

    @Override
    public BlockRegenMaterial createInstance(Type type) {
        return new ItemsAdderMaterial(null);
    }
}
