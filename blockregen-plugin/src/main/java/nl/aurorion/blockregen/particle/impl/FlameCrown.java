package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.particle.ParticleShapes;
import nl.aurorion.blockregen.util.Versions;
import nl.aurorion.blockregen.version.VersionedEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class FlameCrown implements Particle {

    private final BlockRegenPlugin plugin;

    public FlameCrown(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Location center = location.clone().add(0.5, 1.2, 0.5);

        boolean isLegacy = Versions.isCurrentBelow("1.8", true);

        double radius = 0.5;
        double rate = 10;

        if (isLegacy) {
            ParticleShapes.circle(center, radius, rate, (loc) -> {
                plugin.getVersionManager().getMethods().playEffect(loc, VersionedEffect.FLAME);
            });
        } else {
            ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME).withLocation(center);
            Particles.circle(radius, rate, display);
        }
    }
}