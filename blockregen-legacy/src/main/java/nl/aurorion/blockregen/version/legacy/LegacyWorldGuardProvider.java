package nl.aurorion.blockregen.version.legacy;

import com.sk89q.worldguard.bukkit.ProtectionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import nl.aurorion.blockregen.version.api.WorldGuardProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LegacyWorldGuardProvider implements WorldGuardProvider {

    private final WorldGuardPlugin worldGuard;

    public LegacyWorldGuardProvider(WorldGuardPlugin worldGuard) {
        this.worldGuard = worldGuard;
    }

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        ProtectionQuery protectionQuery = worldGuard.createProtectionQuery();
        return protectionQuery.testBlockBreak(player, location.getBlock());
    }

    @Override
    public boolean canTrample(@NotNull Player player, @NotNull Location location) {
        // No block-trampling flag on this version of WG. Simply respect block break.
        return this.canBreak(player, location);
    }
}
