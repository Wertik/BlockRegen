package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.XEntityType;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.particle.Particle;
import nl.aurorion.blockregen.util.Items;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

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

        boolean isLegacy = plugin.getVersionManager().isCurrentBelow("1.9", true);

        if (isLegacy) {
            try {
                location.add(0.5, 0.5, 0.5);
                Firework fw = (Firework) world.spawnEntity(location, XEntityType.FIREWORK_ROCKET.get());
                FireworkMeta fwm = fw.getFireworkMeta();
                
                fwm.addEffect(FireworkEffect.builder()
                        .with(Type.BALL)
                        .withColor(Items.FIREWORK_COLORS.get(random.nextInt(Items.FIREWORK_COLORS.size())))
                        .withFade(Color.WHITE)
                        .flicker(true)
                        .build());
                fw.setFireworkMeta(fwm);
                
                Bukkit.getScheduler().runTaskLaterAsynchronously(BlockRegenPluginImpl.getInstance(), fw::detonate, 2L);
            } catch (Exception e) {
                Location center = location.clone().add(0.5, 0.5, 0.5);
                Color fireworkColor = Items.FIREWORK_COLORS.get(random.nextInt(Items.FIREWORK_COLORS.size()));
                
                for (int i = 0; i < 4; i++) {
                    double angle = (2 * Math.PI * i) / 4;
                    double radius = 0.1;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.playEffect(particleLoc, Effect.POTION_BREAK, fireworkColor.asRGB());
                }
            }
        } else {
            location.add(0.5, 0.5, 0.5);
            Firework fw = (Firework) world.spawnEntity(location, XEntityType.FIREWORK_ROCKET.get());
            FireworkMeta fwm = fw.getFireworkMeta();

            fwm.addEffect(FireworkEffect.builder()
                    .with(Type.BALL)
                    .withColor(Items.FIREWORK_COLORS.get(random.nextInt(Items.FIREWORK_COLORS.size())))
                    .withFade(Color.WHITE)
                    .flicker(true)
                    .build());
            fw.setFireworkMeta(fwm);

            Bukkit.getScheduler().runTaskLaterAsynchronously(BlockRegenPluginImpl.getInstance(), fw::detonate, 2L);
        }
    }
}