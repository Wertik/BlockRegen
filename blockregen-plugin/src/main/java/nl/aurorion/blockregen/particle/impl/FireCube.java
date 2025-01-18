package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.particle.AbstractParticle;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class FireCube extends AbstractParticle {

    @Override
    public String name() {
        return "fire_cube";
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;

        ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME).withLocation(location);
        Particles.cube(location, location, 1, display);
    }
}