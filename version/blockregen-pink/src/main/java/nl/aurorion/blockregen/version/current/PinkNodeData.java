package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PinkPetals;

@Log
@NoArgsConstructor
@Setter
public class PinkNodeData extends LatestNodeData {

    private Integer flowerAmount;

    @Override
    public boolean matches(Block block) {
        boolean sup = super.matches(block);

        if (!sup) {
            return false;
        }

        BlockData data = block.getBlockData();

        if (data instanceof PinkPetals && this.flowerAmount != null) {
            PinkPetals petals = (PinkPetals) data;

            if (petals.getFlowerAmount() != this.flowerAmount) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        super.load(block);

        BlockData data = block.getBlockData();

        if (data instanceof PinkPetals) {
            PinkPetals petals = (PinkPetals) data;
            this.flowerAmount = petals.getFlowerAmount();
        }
    }

    @Override
    public void apply(Block block) {
        super.apply(block);

        BlockData data = block.getBlockData();

        if (data instanceof PinkPetals && this.flowerAmount != null) {
            PinkPetals petals = (PinkPetals) data;
            petals.setFlowerAmount(this.flowerAmount);
        }

        block.setBlockData(data);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && this.flowerAmount == null;
    }

    @Override
    public String toString() {
        return "PinkNodeData{" +
                ".." + super.toString() + ", " +
                "flowerAmount=" + flowerAmount +
                '}';
    }
}
