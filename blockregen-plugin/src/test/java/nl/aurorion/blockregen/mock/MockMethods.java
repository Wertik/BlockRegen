package nl.aurorion.blockregen.mock;

import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.version.api.Methods;
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

    @Override
    public XMaterial getType(@NotNull Block block) throws IllegalArgumentException {
        return XMaterial.AIR;
    }

    @Override
    public ItemStack getItemInMainHand(@NotNull Player player) {
        return null;
    }

    @Override
    public void handleDropItemEvent(Player player, BlockState blockState, List<Item> items) {
        //
    }
}
