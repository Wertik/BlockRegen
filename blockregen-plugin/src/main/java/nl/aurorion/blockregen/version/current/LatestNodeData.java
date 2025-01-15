package nl.aurorion.blockregen.version.current;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileContainerException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.Strings;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Axis;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Stairs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log
@ToString
@NoArgsConstructor
@Setter
public class LatestNodeData implements NodeData {

    private BlockFace facing;

    private Stairs.Shape stairShape;

    private Axis axis;

    private BlockFace rotation;

    private Integer age;

    private String skull;

    // -- Note Blocks

    private Boolean powered;

    private Instrument instrument;
    private Byte noteId;

    // Most resource packs should only use the internal noteId (maybe powered).
    // But just in case include other properties.
    private Integer octave;
    private Note.Tone tone;
    private Boolean sharped;

    // -- Multiface

    private final Set<BlockFace> faces = new HashSet<>();

    public void addFace(BlockFace face) {
        this.faces.add(face);
    }

    public boolean hasFace(BlockFace face) {
        return this.faces.contains(face);
    }

    @Override
    public boolean matches(Block block) {
        BlockData data = block.getBlockData();

        log.fine(() -> String.format("Checking %s against block %s", this, data.getAsString()));

        if (this.skull != null && block.getState() instanceof Skull) {
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

        if (data instanceof Stairs && this.stairShape != null) {
            Stairs stairs = (Stairs) data;
            if (stairs.getShape() != this.stairShape) {
                return false;
            }
        }

        if (data instanceof Orientable && this.axis != null) {
            Orientable orientable = (Orientable) data;
            if (orientable.getAxis() != this.axis) {
                return false;
            }
        }

        if (data instanceof Rotatable && this.rotation != null) {
            Rotatable rotatable = (Rotatable) data;
            if (rotatable.getRotation() != this.rotation) {
                return false;
            }
        }

        if (data instanceof Ageable && this.age != null) {
            Ageable ageable = (Ageable) data;
            if (ageable.getAge() != this.age) {
                return false;
            }
        }

        if (data instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) data;
            if (this.octave != null && this.octave != noteBlock.getNote().getOctave()) {
                return false;
            }

            if (this.noteId != null && this.noteId != noteBlock.getNote().getId()) {
                return false;
            }

            if (this.tone != null && this.tone != noteBlock.getNote().getTone()) {
                return false;
            }

            if (this.sharped != null && this.sharped != noteBlock.getNote().isSharped()) {
                return false;
            }

            if (this.instrument != null && this.instrument != noteBlock.getInstrument()) {
                return false;
            }
        }

        if (data instanceof Powerable) {
            Powerable powerable = (Powerable) data;
            if (this.powered != null && this.powered != powerable.isPowered()) {
                return false;
            }
        }

        if (data instanceof MultipleFacing) {
            MultipleFacing multipleFacing = (MultipleFacing) data;
            // Has to have the exact same faces
            if (!this.faces.isEmpty() && !this.faces.equals(multipleFacing.getFaces())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        BlockData data = block.getBlockData();

        if (block.getState() instanceof Skull) {
            this.skull = XSkull.of(block).getDelegateProfile().getProfileValue();
        }

        if (data instanceof Directional) {
            Directional directional = (Directional) data;
            this.facing = directional.getFacing();
        }

        if (data instanceof Stairs) {
            Stairs stairs = (Stairs) data;
            this.stairShape = stairs.getShape();
        }

        if (data instanceof Orientable) {
            Orientable orientable = (Orientable) data;
            this.axis = orientable.getAxis();
        }

        if (data instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) data;
            this.rotation = rotatable.getRotation();
        }

        if (data instanceof Ageable) {
            Ageable ageable = (Ageable) data;
            this.age = ageable.getAge();
        }

        if (data instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) data;
            this.instrument = noteBlock.getInstrument();
            this.octave = noteBlock.getNote().getOctave();
            this.tone = noteBlock.getNote().getTone();
            this.sharped = noteBlock.getNote().isSharped();
            this.noteId = noteBlock.getNote().getId();
        }

        if (data instanceof Powerable) {
            Powerable powerable = (Powerable) data;
            this.powered = powerable.isPowered();
        }

        if (data instanceof MultipleFacing) {
            MultipleFacing multipleFacing = (MultipleFacing) data;
            this.faces.clear();
            this.faces.addAll(multipleFacing.getFaces());
        }

        log.fine(() -> String.format("Loaded block data %s (%s)", block.getType(), this));
    }

    @Override
    public void apply(Block block) {
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Directional && this.facing != null) {
            ((Directional) blockData).setFacing(this.facing);
        }

        if (blockData instanceof Stairs && this.stairShape != null) {
            ((Stairs) blockData).setShape(this.stairShape);
        }

        if (blockData instanceof Orientable && this.axis != null) {
            ((Orientable) blockData).setAxis(this.axis);
        }

        if (blockData instanceof Rotatable && this.rotation != null) {
            ((Rotatable) blockData).setRotation(this.rotation);
        }

        if (blockData instanceof Ageable && this.age != null) {
            Ageable ageable = (Ageable) blockData;
            if (this.age > ageable.getMaximumAge()) {
                ageable.setAge(ageable.getMaximumAge());
            } else {
                ageable.setAge(this.age);
            }
        }

        if (blockData instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) blockData;
            if (this.instrument != null) {
                noteBlock.setInstrument(this.instrument);
            }

            if (this.noteId != null) {
                Note note = new Note(this.noteId);
                noteBlock.setNote(note);
            }

            if (this.tone != null && this.octave != null) {
                Note note = new Note(this.octave, this.tone, this.sharped != null && sharped);
                noteBlock.setNote(note);
            }
        }

        if (blockData instanceof Powerable) {
            Powerable powerable = (Powerable) blockData;
            if (this.powered != null) {
                powerable.setPowered(this.powered);
            }
        }

        if (blockData instanceof MultipleFacing) {
            MultipleFacing multipleFacing = (MultipleFacing) blockData;
            if (!this.faces.isEmpty()) {
                for (BlockFace face : multipleFacing.getAllowedFaces()) {
                    multipleFacing.setFace(face, this.faces.contains(face));
                }
            }
        }

        block.setBlockData(blockData);

        if (this.skull != null && block.getState() instanceof Skull) {
            XSkull.of(block)
                    .profile(Profileable.detect(this.skull))
                    .apply();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.facing == null &&
                this.stairShape == null &&
                this.axis == null &&
                this.rotation == null &&
                this.age == null &&
                this.instrument == null &&
                this.octave == null &&
                this.noteId == null &&
                this.tone == null &&
                this.sharped == null &&
                this.powered == null &&
                this.faces.isEmpty();
    }

    @Override
    public String getPrettyString() {
        Map<String, Object> entries = new HashMap<>();
        entries.put("facing", this.facing);
        entries.put("shape", this.stairShape);
        entries.put("axis", this.axis);
        entries.put("rotation", this.rotation);
        entries.put("age", this.age);
        entries.put("skull", this.skull);
        entries.put("noteId", this.noteId);
        entries.put("octave", this.octave);
        entries.put("tone", this.tone);
        entries.put("instrument", this.instrument);
        entries.put("sharped", this.sharped);

        String serialized = Strings.serializeNodeDataEntries(entries);
        if (!this.faces.isEmpty()) {
            log.fine(serialized);
            String faces = this.faces.stream().map(face -> String.format("%s=true", face)).collect(Collectors.joining(","));
            serialized = serialized.substring(0, serialized.length() - 1) + faces + "]";
        }
        return serialized;
    }
}
