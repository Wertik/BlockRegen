package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Versions;
import nl.aurorion.blockregen.version.VersionedEffect;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class WitchSpell implements Particle {

    private final BlockRegenPlugin plugin;

    public WitchSpell(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Location center = location.clone().add(0.5, 0.5, 0.5);

        boolean isLegacy = Versions.isCurrentBelow("1.8", true);

        double radius = 0.5;
        double rate = 10;

        if (isLegacy) {
            double theta = 2 * Math.PI / rate;
            double angle = 0.0;
            while (angle < (2 * Math.PI)) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                plugin.getVersionManager().getMethods().playEffect(center.clone().add(x, 0, z), VersionedEffect.WITCH_SPELL);

                angle += theta;
            }
        } else {
            ParticleDisplay display = ParticleDisplay.of(XParticle.WITCH).withLocation(center);
            Particles.circle(radius, rate, display);
        }
    }
}