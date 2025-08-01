package nl.aurorion.blockregen.mock;

import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.version.api.Methods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MockMethods implements Methods {
    @Override
    public void setType(@NotNull Block block, @NotNull XMaterial xMaterial) {
        //
    }

    /**
     * @throws ParseException If it fails.
     */
    @Override
    public @NotNull XMaterial getType(@NotNull Block block) {
        return XMaterial.AIR;
    }

    @Override
    public @NotNull ItemStack getItemInMainHand(@NotNull Player player) {
        return new ItemStack(Material.AIR);
    }

    @Override
    public void handleDropItemEvent(Player player, BlockState blockState, List<Item> items) {
        //
    }

    @Override
    public int applyMending(Player player, int experience) {
        return experience;
    }
}
