package nl.aurorion.blockregen.compatibility.impl;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.BlockActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.JobsPlayer;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Log
public class JobsProvider extends CompatibilityProvider {

    public JobsProvider(BlockRegenPlugin plugin) {
        super(plugin);
        setFeatures("rewards", "conditions");
    }

    @Override
    public void onLoad() {
        // todo
        // plugin.getPresetManager().getConditions().register(getPrefix() + "/job", JobsCondition.parser());
    }

    public void triggerBlockBreakAction(Player player, Block block) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        Jobs.action(jobsPlayer, new BlockActionInfo(block, ActionType.BREAK), block);
    }
}