package nl.aurorion.blockregen.system.preset.struct.material;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.Mechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NexoMaterial implements TargetMaterial {

    private final BlockRegen plugin;

    @Getter
    private final String itemId;

    public NexoMaterial(BlockRegen plugin, String itemId) {
        this.plugin = plugin;
        this.itemId = itemId;
    }

    // Get the mechanic no matter which one the block uses.
    @Nullable
    public static Mechanic getNexoBlock(@NotNull Block block) {
        Mechanic mechanic;
        if (!NexoBlocks.isCustomBlock(block)) {
            mechanic = null;
        } else {
            switch (block.getType()) {
                case NOTE_BLOCK:
                    mechanic = NexoBlocks.noteBlockMechanic(block);
                    break;
                case TRIPWIRE:
                    mechanic = NexoBlocks.stringMechanic(block);
                    break;
                case MUSHROOM_STEM:
                default:
                    mechanic = NexoBlocks.customBlockMechanic(block.getBlockData());
                    break;
            }
        }
        return mechanic;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        Mechanic mechanic = getNexoBlock(block);
        if (mechanic == null) {
            return false;
        }
        return Objects.equals(mechanic.getItemID(), itemId);
    }

    @Override
    public void place(Block block) {
        NexoBlocks.place(this.itemId, block.getLocation());
    }

    @Override
    public String toString() {
        return "NexoMaterial{" +
                "itemId='" + itemId + '\'' +
                '}';
    }
}