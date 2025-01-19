package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.particle.Particle;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class FireCube implements Particle {

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return;
        }

        // Slightly overhang the block to make it look better.
        Location end = location.clone().add(1.2, 1.2, 1.2);

        ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME);
        Particles.structuredCube(location, end, 0.2, display);
    }
}