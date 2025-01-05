package nl.aurorion.blockregen.version.latest;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.ProtectionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import nl.aurorion.blockregen.version.api.WorldGuardProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LatestWorldGuardProvider implements WorldGuardProvider {

    private final WorldGuardPlugin worldGuard;

    public LatestWorldGuardProvider(WorldGuardPlugin worldGuard) {
        this.worldGuard = worldGuard;
    }

    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        ProtectionQuery protectionQuery = worldGuard.createProtectionQuery();
        return protectionQuery.testBlockBreak(player, location.getBlock());
    }

    @Override
    public boolean canTrample(@NotNull Player player, @NotNull Location location) {
        // Check that the trampling flag is set to allow for this region.
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldedit.util.Location loc = new com.sk89q.worldedit.util.Location(localPlayer.getWorld(), location.getX(), location.getY(), location.getZ());
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        return query.testState(loc, localPlayer, Flags.TRAMPLE_BLOCKS);
    }
}