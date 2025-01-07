package nl.aurorion.blockregen.particle.impl;

import com.cryptomorin.xseries.XEntityType;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.util.Items;
import nl.aurorion.blockregen.particle.AbstractParticle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class FireWorks extends AbstractParticle {

    private final Random random = new Random();

    @Override
    public String name() {
        return "fireworks";
    }

    @Override
    public void display(@NotNull Location location) {
        World world = location.getWorld();

        if (world == null) return;

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