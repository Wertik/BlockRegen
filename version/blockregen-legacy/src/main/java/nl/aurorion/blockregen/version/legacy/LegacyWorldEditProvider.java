package nl.aurorion.blockregen.version.legacy;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import nl.aurorion.blockregen.region.selection.RegionSelection;
import nl.aurorion.blockregen.version.api.WorldEditProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LegacyWorldEditProvider implements WorldEditProvider {

    private final WorldEditPlugin worldEditPlugin;

    public LegacyWorldEditProvider(WorldEditPlugin worldEditPlugin) {
        this.worldEditPlugin = worldEditPlugin;
    }

    @Override
    public @Nullable RegionSelection createSelection(@NotNull Player player) {
        Selection selection = worldEditPlugin.getSelection(player);

        if (selection == null || selection.getWorld() == null)
            return null;

        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();

        return new RegionSelection(min, max);
    }
}
