package nl.aurorion.blockregen.system.event.struct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.system.preset.NumberValue;
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
    private NumberValue itemRarity;

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

    @Override
    public String toString() {
        return "PresetEvent{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", doubleDrops=" + doubleDrops +
                ", doubleExperience=" + doubleExperience +
                ", itemRarity=" + itemRarity +
                ", item=" + item +
                ", rewards=" + rewards +
                ", bossBar=" + bossBar +
                ", enabled=" + enabled +
                '}';
    }
}