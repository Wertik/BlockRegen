package nl.aurorion.blockregen.version.current;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import nl.aurorion.blockregen.system.region.struct.RegionSelection;
import nl.aurorion.blockregen.version.api.WorldEditProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LatestWorldEditProvider implements WorldEditProvider {

    private final WorldEditPlugin worldEdit;

    public LatestWorldEditProvider(WorldEditPlugin worldEdit) {
        this.worldEdit = worldEdit;
    }

    public Region getSelection(@NotNull Player player) {
        Region selection;
        try {
            selection = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            return null;
        }
        return selection;
    }

    @Nullable
    public RegionSelection createSelection(@NotNull Player player) {

        Region selection = getSelection(player);

        if (selection == null || selection.getWorld() == null) {
            return null;
        }

        World world = BukkitAdapter.adapt(selection.getWorld());

        Location min = BukkitAdapter.adapt(world, selection.getMinimumPoint());
        Location max = BukkitAdapter.adapt(world, selection.getMaximumPoint());

        return new RegionSelection(min, max);
    }
}