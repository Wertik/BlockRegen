package nl.aurorion.blockregen.particle;

import nl.aurorion.blockregen.BlockRegen;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractParticle {

    public abstract void display(@NotNull Location location);

    public abstract String name();

    public void register() {
        BlockRegen.getInstance().getParticleManager().addParticle(name(), this);
    }
}