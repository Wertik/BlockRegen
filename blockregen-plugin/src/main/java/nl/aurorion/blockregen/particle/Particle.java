package nl.aurorion.blockregen.particle;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface Particle {
    /**
     * Display a particle at the given {@link Location}.
     *
     * @param location Location of the block (coordinates are rounded).
     */
    void display(@NotNull Location location);
}