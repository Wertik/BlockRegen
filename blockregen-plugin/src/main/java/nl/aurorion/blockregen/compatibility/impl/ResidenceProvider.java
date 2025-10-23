package nl.aurorion.blockregen.compatibility.impl;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.regeneration.RegenerationEventType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Log
public class ResidenceProvider extends CompatibilityProvider {

    public ResidenceProvider(BlockRegenPlugin plugin) {
        super(plugin);
    }

    public boolean canBreak(Block block, Player player, RegenerationEventType type) {
        ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(block.getLocation());

        if (residence != null) {
            ResidencePermissions permissions = residence.getPermissions();

            if (type == RegenerationEventType.BLOCK_BREAK) {
                // has neither build nor destroy
                // let residence run its protection
                if (!permissions.playerHas(player, Flags.destroy, true) &&
                        !permissions.playerHas(player, Flags.build, true)) {
                    log.fine(() -> "Let Residence handle block break.");
                    return false;
                }
            } else if (type == RegenerationEventType.TRAMPLING) {
                if (!permissions.playerHas(player, Flags.trample, true)) {
                    log.fine(() -> "Let Residence handle trample.");
                    return false;
                }
            }
        }
        return true;
    }
}
