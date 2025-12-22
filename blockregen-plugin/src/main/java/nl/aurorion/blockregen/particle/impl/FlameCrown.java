package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Versions;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
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

        Location start = location.clone().add(.5, 1.2, .5);

        boolean isLegacy = Versions.isCurrentBelow("1.9", true);

        if (isLegacy) {
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;
                Location particleLoc = start.clone().add(x, 0, z);
                world.playEffect(particleLoc, Effect.MOBSPAWNER_FLAMES, 0);
            }
        } else {
            ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME).withLocation(start);
            Bukkit.getScheduler().runTaskAsynchronously(BlockRegenPluginImpl.getInstance(), () -> Particles.circle(.5, 10, display));
        }
    }
}