package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Versions;
import nl.aurorion.blockregen.version.VersionedEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class FireCube implements Particle {

    private final BlockRegenPlugin plugin;

    public FireCube(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return;
        }

        boolean isLegacy = Versions.isCurrentBelow("1.8", true);

        Location start = location.clone();
        Location end = location.clone().add(1.2, 1.2, 1.2);
        double spacing = 0.2;

        if (isLegacy) {
            for (double x = start.getX(); x <= end.getX(); x += spacing) {
                for (double y = start.getY(); y <= end.getY(); y += spacing) {
                    for (double z = start.getZ(); z <= end.getZ(); z += spacing) {
                        int components = 0;
                        if (x == start.getX() || x + spacing > end.getX()) {
                            ++components;
                        }

                        if (y == start.getY() || y + spacing > end.getY()) {
                            ++components;
                        }

                        if (z == start.getZ() || z + spacing > end.getZ()) {
                            ++components;
                        }

                        if (components >= 2) {
                            plugin.getVersionManager().getMethods().playEffect(new Location(world, x, y, z), VersionedEffect.FLAME);
                        }
                    }
                }
            }
        } else {
            ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> Particles.structuredCube(location, end, spacing, display));
        }
    }
}