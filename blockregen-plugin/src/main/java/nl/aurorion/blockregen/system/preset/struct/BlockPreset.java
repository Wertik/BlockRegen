package nl.aurorion.blockregen.system.preset.struct;

import com.cryptomorin.xseries.XSound;
import lombok.Data;
import nl.aurorion.blockregen.system.preset.struct.material.DynamicMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

@Data
public class BlockPreset {

    private final String name;

    private TargetMaterial targetMaterial;

    private DynamicMaterial replaceMaterial;
    private DynamicMaterial regenMaterial;

    private Amount delay;

    private String particle;
    private String regenerationParticle;

    private boolean naturalBreak;
    private boolean applyFortune;
    private boolean dropNaturally;

    private PresetConditions conditions;
    private PresetRewards rewards;

    private XSound sound;

    public BlockPreset(String name) {
        this.name = name;
    }
}