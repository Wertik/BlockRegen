package nl.aurorion.blockregen.particle;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ParticleShapes {

    public static void circle(@NotNull Location center, double radius, double rate, @NotNull Consumer<Location> playEffect) {
        double theta = 2 * Math.PI / rate;
        double angle = 0.0;
        while (angle < (2 * Math.PI)) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            playEffect.accept(center.clone().add(x, 0, z));

            angle += theta;
        }
    }
}
