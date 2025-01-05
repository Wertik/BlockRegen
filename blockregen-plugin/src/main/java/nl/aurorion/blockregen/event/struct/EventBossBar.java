package nl.aurorion.blockregen.event.struct;

import lombok.Data;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Log
@Data
public class EventBossBar {

    private String text;
    private String color;
    private String style;

    @Nullable
    public static EventBossBar load(@Nullable ConfigurationSection section, @NotNull String defaultText) {

        if (section == null)
            return null;

        EventBossBar bossBar = new EventBossBar();

        bossBar.setText(section.getString("name", defaultText));

        String barStyle = section.getString("style", "SOLID");

        if (!BlockRegen.getInstance().getVersionManager().getMethods().isBarStyleValid(barStyle)) {
            log.warning("Boss bar style " + barStyle + " is invalid, using SOLID as default.");
            bossBar.setStyle("SOLID");
        } else bossBar.setStyle(barStyle);

        String barColor = section.getString("color", "BLUE");

        if (!BlockRegen.getInstance().getVersionManager().getMethods().isBarColorValid(barColor)) {
            log.warning("Boss bar color " + barColor + " is invalid, using BLUE as default.");
            bossBar.setColor("BLUE");
        } else bossBar.setColor(barColor);

        return bossBar;
    }
}