package nl.aurorion.blockregen.compatibility.provider;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.BlockActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import nl.aurorion.blockregen.conditional.Condition;
import nl.aurorion.blockregen.Context;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import nl.aurorion.blockregen.preset.condition.expression.Expression;
import nl.aurorion.blockregen.preset.condition.expression.Operand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Log
public class JobsProvider extends CompatibilityProvider {

    public JobsProvider(BlockRegenPlugin plugin) {
        super(plugin, "jobs");
        setFeatures("rewards", "conditions");
    }

    @Override
    public void onLoad() {
        plugin.getPresetManager().getConditions().addProvider(getPrefix() + "/levels", (key, node) -> {
            String v = (String) node;

            Expression expression = Expression.withCustomOperands(JobsProvider::getJobOperand, v);
            log.fine(() -> "Loaded jobs expression " + expression);
            return Condition.of(expression::evaluate).alias(v);
        }).extender((ctx) -> {
            Player player = (Player) ctx.mustVar("player");
            JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
            return Context.of("jobs.player", jobsPlayer);
        });
    }

    @NotNull
    private static Operand getJobOperand(@NotNull String str) {
        Job job = Jobs.getJob(str);

        if (job == null) {
            throw new ParseException("Invalid job '" + str + "'");
        }

        return (ctx) -> {
            JobsPlayer player = (JobsPlayer) ctx.mustVar("jobs.player");
            JobProgression progression = player.getJobProgression(job);
            return progression == null ? 0 : progression.getLevel();
        };
    }

    public void triggerBlockBreakAction(Player player, Block block) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        Jobs.action(jobsPlayer, new BlockActionInfo(block, ActionType.BREAK), block);
    }
}