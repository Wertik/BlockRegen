package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.XEntityType;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Colors;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@Log
public class FireWorks implements Particle {

    private final BlockRegenPlugin plugin;
    private final Random random = new Random();

    public FireWorks(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return;
        }

        location.add(0.5, 0.5, 0.5);
        EntityType entityType = XEntityType.FIREWORK_ROCKET.get();

        if (entityType == null) {
            log.warning("Fireworks are not supported on this version.");
            return;
        }

        Firework fw = (Firework) world.spawnEntity(location, entityType);

        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.addEffect(FireworkEffect.builder()
                .with(Type.BALL)
                .withColor(Colors.FIREWORK_COLORS.get(random.nextInt(Colors.FIREWORK_COLORS.size())))
                .withFade(Color.WHITE)
                .flicker(true)
                .build());
        fw.setFireworkMeta(fwm);

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, fw::detonate, 2L);
    }
}