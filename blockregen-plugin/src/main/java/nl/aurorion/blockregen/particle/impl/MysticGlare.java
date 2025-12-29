package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.particle.ParticleShapes;
import nl.aurorion.blockregen.util.BukkitVersions;
import nl.aurorion.blockregen.version.VersionedEffect;
import org.bukkit.Location;
import org.jspecify.annotations.NonNull;

public class MysticGlare implements Particle {

    private final BlockRegenPlugin plugin;

    public MysticGlare(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void display(@NonNull Location location) {
        double rate = 10;
        double radius = 0.5;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        if (BukkitVersions.isCurrentBelow("1.8", true)) {
            ParticleShapes.circle(center, radius, rate, (loc) -> {
                plugin.getVersionManager().getMethods().playEffect(loc, VersionedEffect.EFFECT);
            });
        } else {
            ParticleDisplay display = ParticleDisplay.of(XParticle.EFFECT).withLocation(center);
            Particles.circle(0.5, 10, display);
        }
    }
}
