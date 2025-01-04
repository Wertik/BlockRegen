package nl.aurorion.blockregen.system.event.struct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.system.preset.Amount;
import nl.aurorion.blockregen.system.preset.PresetRewards;
import nl.aurorion.blockregen.system.preset.drop.DropItem;
import org.bukkit.boss.BossBar;
import org.jetbrains.annotations.Nullable;

@Getter
@Log
public class PresetEvent {

    private final String name;
    @Setter
    private String displayName;

    @Setter
    private boolean doubleDrops;
    @Setter
    private boolean doubleExperience;

    @Setter
    private Amount itemRarity;

    @Setter
    @Nullable
    private DropItem item;

    @Setter
    private PresetRewards rewards = new PresetRewards();

    @Setter
    private EventBossBar bossBar;

    @Setter
    private BossBar activeBossBar;

    @Setter
    private boolean enabled = false;

    public PresetEvent(String name) {
        this.name = name;
    }
}