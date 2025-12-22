package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Versions;
import org.bukkit.Effect;
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

        boolean isLegacy = Versions.isCurrentBelow("1.9", true);

        if (isLegacy) {
            Location start = location.clone();
            Location end = location.clone().add(1.0, 1.0, 1.0);
            double spacing = 0.3; 
            
            for (double x = start.getX(); x <= end.getX(); x += spacing) {
                for (double y = start.getY(); y <= end.getY(); y += spacing) {
                    for (double z = start.getZ(); z <= end.getZ(); z += spacing) {
                        boolean isEdge = (x <= start.getX() + 0.15 || x >= end.getX() - 0.15) ||
                                        (y <= start.getY() + 0.15 || y >= end.getY() - 0.15) ||
                                        (z <= start.getZ() + 0.15 || z >= end.getZ() - 0.15);
                        if (isEdge) {
                            world.playEffect(new Location(world, x, y, z), Effect.MOBSPAWNER_FLAMES, 0);
                        }
                    }
                }
            }
        } else {
            // modern XParticle API.
            Location end = location.clone().add(1.2, 1.2, 1.2);
            ParticleDisplay display = ParticleDisplay.of(XParticle.FLAME);
            Particles.structuredCube(location, end, 0.2, display);
        }
    }
}