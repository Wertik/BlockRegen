package nl.aurorion.blockregen.version.ancient;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileContainerException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.*;

import java.util.HashMap;
import java.util.Map;

@Log
@ToString
@Setter
@NoArgsConstructor
public class AncientNodeData implements NodeData {

    private BlockFace facing;

    // Trees
    private TreeSpecies treeSpecies;

    // Stairs
    private Boolean inverted;

    private CropState cropState;

    private String skull;

    @Override
    public boolean check(Block block) {
        MaterialData data = block.getState().getData();

        if (this.skull != null) {
            try {
                String profileString = XSkull.of(block).getDelegateProfile().getProfileValue();

                if (profileString != null && !profileString.equals(this.skull)) {
                    return false;
                }
            } catch (InvalidProfileContainerException e) {
                // not a skull
                return false;
            }
        }

        if (data instanceof Directional && this.facing != null) {
            Directional directional = (Directional) data;
            if (directional.getFacing() != this.facing) {
                return false;
            }
        }

        if (data instanceof Tree && this.facing != null) {
            Tree tree = (Tree) data;
            if (tree.getDirection() != this.facing) {
                return false;
            }

            if (tree.getSpecies() != this.treeSpecies) {
                return false;
            }
        }

        if (data instanceof Stairs && this.inverted != null) {
            Stairs stairs = (Stairs) data;
            if (stairs.isInverted() != this.inverted) {
                return false;
            }
        }

        if (data instanceof Crops && this.cropState != null) {
            Crops crops = (Crops) data;
            if (crops.getState() != this.cropState) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        MaterialData data = block.getState().getData();

        try {
            this.skull = XSkull.of(block).getDelegateProfile().getProfileValue();
        } catch (InvalidProfileContainerException e) {
            // not a skull
        }

        if (data instanceof Directional) {
            Directional directional = (Directional) data;
            this.facing = directional.getFacing();
        }

        if (data instanceof Tree) {
            Tree tree = (Tree) data;
            this.facing = tree.getDirection();
            this.treeSpecies = tree.getSpecies();
        }

        if (data instanceof Stairs) {
            Stairs stairs = (Stairs) data;
            this.inverted = stairs.isInverted();
        }

        if (data instanceof Crops) {
            Crops crops = (Crops) data;
            this.cropState = crops.getState();
        }

        log.fine(() -> String.format("Loaded block data %s (%s)", block.getType(), this));
    }

    @Override
    public void place(Block block) {
        BlockState state = block.getState();
        MaterialData data = state.getData();

        if (data instanceof Directional && this.facing != null) {
            Directional directional = (Directional) data;
            directional.setFacingDirection(this.facing);
        }

        if (data instanceof Tree) {
            Tree tree = (Tree) data;
            if (this.facing != null) {
                tree.setDirection(this.facing);
            }

            if (this.treeSpecies != null) {
                tree.setSpecies(this.treeSpecies);
            }
        }

        if (data instanceof Stairs && this.inverted != null && this.inverted) {
            Stairs stairs = (Stairs) data;
            stairs.setInverted(true);
        }

        if (data instanceof Crops && this.cropState != null) {
            Crops crops = (Crops) data;
            crops.setState(cropState);
        }

        if (this.skull != null) {
            XSkull.of(block)
                    .profile(Profileable.detect(this.skull))
                    .apply();
        }

        state.setData(data);
    }

    @Override
    public boolean isEmpty() {
        return this.facing == null && this.treeSpecies == null && this.inverted == null && this.cropState == null;
    }

    @Override
    public String getPrettyString() {
        Map<String, Object> entries = new HashMap<>();
        entries.put("facing", this.facing);
        entries.put("species", this.treeSpecies);
        entries.put("inverted", this.inverted);
        entries.put("age", this.cropState == null ? null : this.cropState.ordinal());
        return StringUtil.serializeNodeDataEntries(entries);
    }
}
