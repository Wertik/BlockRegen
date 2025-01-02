package nl.aurorion.blockregen.version.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface WorldGuardProvider {

    boolean canBreak(@NotNull Player player, @NotNull Location location);

    boolean canTrample(@NotNull Player player, @NotNull Location location);
}
