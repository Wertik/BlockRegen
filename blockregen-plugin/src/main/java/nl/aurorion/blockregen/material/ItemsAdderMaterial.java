package nl.aurorion.blockregen.material;

import dev.lone.itemsadder.api.CustomBlock;
import lombok.extern.java.Log;
import org.bukkit.block.Block;

@Log
public class ItemsAdderMaterial implements BlockRegenMaterial {

    private final String id;

    public ItemsAdderMaterial(String id) {
        this.id = id;
    }

    @Override
    public boolean check(Block block) {
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);

        if (customBlock == null) {
            return false;
        }

        String placedId = customBlock.getNamespacedID();
        return id.equals(placedId);
    }

    @Override
    public void setType(Block block) {
        CustomBlock customBlock = CustomBlock.getInstance(this.id);
        customBlock.place(block.getLocation());
    }

    @Override
    public String toString() {
        return "ItemsAdderMaterial{" +
                "id='" + id + '\'' +
                '}';
    }
}
