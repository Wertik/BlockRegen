package nl.aurorion.blockregen.version.api;


import nl.aurorion.blockregen.region.RegionSelection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WorldEditProvider {

    @Nullable RegionSelection createSelection(@NotNull Player player);
}
