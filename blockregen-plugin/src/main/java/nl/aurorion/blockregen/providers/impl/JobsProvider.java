package nl.aurorion.blockregen.providers.impl;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.BlockActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.JobsPlayer;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.CompatibilityProvider;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Log
public class JobsProvider extends CompatibilityProvider {

    public JobsProvider(BlockRegen plugin) {
        super(plugin);
        setFeatures("rewards", "conditions");
    }

    public void triggerBlockBreakAction(Player player, Block block) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        Jobs.action(jobsPlayer, new BlockActionInfo(block, ActionType.BREAK), block);
    }
}