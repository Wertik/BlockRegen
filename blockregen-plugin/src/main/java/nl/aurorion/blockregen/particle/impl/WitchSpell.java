package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.particle.Particle;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class WitchSpell implements Particle {

    @Override
    public void display(@NotNull Location location) {
        ParticleDisplay display = ParticleDisplay.of(XParticle.WITCH).withLocation(location.clone().add(.5, .5, .5));
        Particles.circle(.5, 10, display);
    }
}