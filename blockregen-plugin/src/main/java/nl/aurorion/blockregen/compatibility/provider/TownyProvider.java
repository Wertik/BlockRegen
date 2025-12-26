package nl.aurorion.blockregen.compatibility.provider;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Log
public class TownyProvider extends CompatibilityProvider {
    public TownyProvider(BlockRegenPlugin plugin) {
        super(plugin);
    }

    public boolean canBreak(Block block, Player player) {
        return PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.DESTROY);
    }
}
