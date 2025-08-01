package nl.aurorion.blockregen.preset;

import com.cryptomorin.xseries.XSound;
import nl.aurorion.blockregen.conditional.Condition;
import lombok.Data;
import nl.aurorion.blockregen.preset.material.PlacementMaterial;
import nl.aurorion.blockregen.preset.material.TargetMaterial;
import org.jetbrains.annotations.Nullable;

@Data
public class BlockPreset {

    private final String name;

    private TargetMaterial targetMaterial;

    @Nullable
    private PlacementMaterial replaceMaterial;
    @Nullable
    private PlacementMaterial regenMaterial;

    private NumberValue delay;

    private String particle;
    private String regenerationParticle;

    private boolean naturalBreak;
    private boolean applyFortune;
    private boolean dropNaturally;

    // Disable physics of neighbouring blocks
    private boolean disablePhysics;

    // Specific handling for crops (cactus, sugarcane, wheat,...)
    private boolean handleCrops;
    // Require solid ground under blocks that require it (cactus, sugarcane, wheat,...)
    private boolean checkSolidGround;

    // Regenerate the whole multiblock crop
    private boolean regenerateWhole;

    /**
     * @deprecated in favor of composed conditions {@link BlockPreset#condition}.
     * */
    @Deprecated()
    private PresetConditions conditions;
    private PresetRewards rewards;

    private Condition condition;

    private XSound sound;

    public BlockPreset(String name) {
        this.name = name;
    }
}