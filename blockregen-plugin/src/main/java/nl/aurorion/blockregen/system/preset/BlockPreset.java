package nl.aurorion.blockregen.system.preset;

import com.cryptomorin.xseries.XSound;
import lombok.Data;
import nl.aurorion.blockregen.system.preset.material.PlacementMaterial;
import nl.aurorion.blockregen.system.preset.material.TargetMaterial;
import org.jetbrains.annotations.Nullable;

@Data
public class BlockPreset {

    private final String name;

    private TargetMaterial targetMaterial;

    @Nullable
    private PlacementMaterial replaceMaterial;
    @Nullable
    private PlacementMaterial regenMaterial;

    private Amount delay;

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

    private PresetConditions conditions;
    private PresetRewards rewards;

    private XSound sound;

    public BlockPreset(String name) {
        this.name = name;
    }
}