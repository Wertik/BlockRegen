package nl.aurorion.blockregen.particle;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log
public class ParticleManager {

    private final Map<String, Particle> particles = new HashMap<>();

    public void displayParticle(@NotNull String particleName, @NotNull Block block) {
        Location location = block.getLocation();

        if (!particles.containsKey(particleName)) {
            return;
        }

        particles.get(particleName).display(location);
        log.fine(() -> "Displayed particle " + particleName + " at location " + Locations.locationToString(location));
    }

    public void addParticle(String name, Particle particle) {
        particles.put(name, particle);
    }

    public Map<String, Particle> getParticles() {
        return Collections.unmodifiableMap(particles);
    }

    public Particle getParticle(String name) {
        return particles.getOrDefault(name, null);
    }
}