package nl.aurorion.blockregen.compatibility.material;

import com.cryptomorin.xseries.XMaterial;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NexoMaterial implements BlockRegenMaterial {

    private final transient BlockRegenPlugin plugin;

    @Getter
    private final String itemId;

    public NexoMaterial(BlockRegenPlugin plugin, String itemId) {
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
    public boolean check(Block block) {
        Mechanic mechanic = getNexoBlock(block);
        if (mechanic == null) {
            return false;
        }
        return Objects.equals(mechanic.getItemID(), itemId);
    }

    @Override
    public void setType(Block block) {
        NexoBlocks.place(this.itemId, block.getLocation());
    }

    @Override
    public XMaterial getType() {
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(this.itemId);
        if (mechanic == null) {
            throw new IllegalArgumentException(String.format("Invalid next block: %s", this.itemId));
        }
        return XMaterial.matchXMaterial(mechanic.getBlockData().getMaterial());
    }

    @Override
    public String toString() {
        return "NexoMaterial{" +
                "itemId='" + itemId + '\'' +
                '}';
    }
}
