package nl.aurorion.blockregen.compatibility.impl;

import lombok.Getter;
import lombok.extern.java.Log;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Log
public class GriefPreventionProvider extends CompatibilityProvider {

    @Getter
    private GriefPrevention griefPrevention;

    public GriefPreventionProvider(BlockRegenPlugin plugin) {
        super(plugin);
    }

    public boolean canBreak(Block block, Player player) {
        String noBuildReason = griefPrevention.allowBreak(player, block, block.getLocation(), null);

        if (noBuildReason != null) {
            log.fine(() -> "Let GriefPrevention handle this.");
            return false;
        }
        return true;
    }

    @Override
    public void onLoad() {
        this.griefPrevention = GriefPrevention.instance;
    }
}
