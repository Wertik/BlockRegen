package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.BukkitVersions;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.FlowerBed;
import org.bukkit.block.data.type.PinkPetals;

@Log
@NoArgsConstructor
@Setter
public class PinkNodeData extends LatestNodeData {

    private final static boolean FLOWER_BED = BukkitVersions.isCurrentAbove("1.21.5", true);

    private Integer flowerAmount;

    @Override
    public boolean matches(Block block) {
        boolean sup = super.matches(block);

        if (!sup) {
            return false;
        }

        BlockData data = block.getBlockData();

        if (FLOWER_BED) {
            if (data instanceof FlowerBed && this.flowerAmount != null) {
                FlowerBed bed = (FlowerBed) data;
                if (bed.getFlowerAmount() != this.flowerAmount) {
                    return false;
                }
            }
        } else {
            if (data instanceof PinkPetals && this.flowerAmount != null) {
                PinkPetals petals = (PinkPetals) data;

                if (petals.getFlowerAmount() != this.flowerAmount) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        super.load(block);

        BlockData data = block.getBlockData();

        if (FLOWER_BED) {
            if (data instanceof FlowerBed) {
                FlowerBed flowerBed = (FlowerBed) data;
                this.flowerAmount = flowerBed.getFlowerAmount();
            }
        } else {
            if (data instanceof PinkPetals) {
                PinkPetals petals = (PinkPetals) data;
                this.flowerAmount = petals.getFlowerAmount();
            }
        }
    }

    @Override
    public void apply(Block block) {
        super.apply(block);

        BlockData data = block.getBlockData();

        if (FLOWER_BED) {
            if (data instanceof FlowerBed && this.flowerAmount != null) {
                FlowerBed flowerBed = (FlowerBed) data;
                flowerBed.setFlowerAmount(this.flowerAmount);
            }
        } else {
            if (data instanceof PinkPetals && this.flowerAmount != null) {
                PinkPetals petals = (PinkPetals) data;
                petals.setFlowerAmount(this.flowerAmount);
            }
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
