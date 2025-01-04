package nl.aurorion.blockregen.system.preset.struct;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.google.common.base.Strings;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log
@NoArgsConstructor
public class PresetConditions {

    private final List<XMaterial> toolsRequired = new ArrayList<>();

    private final Map<XEnchantment, Integer> enchantsRequired = new HashMap<>();

    private final Map<Job, Integer> jobsRequired = new HashMap<>();

    public boolean check(Player player) {
        return checkTools(player) && checkEnchants(player) && checkJobs(player);
    }

    public boolean checkTools(Player player) {

        if (toolsRequired.isEmpty())
            return true;

        ItemStack tool = BlockRegen.getInstance().getVersionManager().getMethods().getItemInMainHand(player);

        if (toolsRequired.contains(XMaterial.matchXMaterial(tool)))
            return true;

        String requirements = composeToolRequirements();

        player.sendMessage(Message.TOOL_REQUIRED_ERROR.get(player)
                .replace("%tool%", requirements));
        log.fine(() -> String.format("Player doesn't have the required tools. Tool: %s, required: %s",
                tool.getType(), requirements));
        return false;
    }

    private String composeToolRequirements() {
        return toolsRequired.stream()
                .map(xMaterial -> TextUtil.capitalize(xMaterial.toString()
                        .toLowerCase()
                        .replace("_", " ")))
                .collect(Collectors.joining(", "));
    }

    public boolean checkEnchants(Player player) {

        if (enchantsRequired.isEmpty())
            return true;

        ItemStack tool = BlockRegen.getInstance().getVersionManager().getMethods().getItemInMainHand(player);

        ItemMeta meta = tool.getItemMeta();

        String requirements = compressEnchantRequirements();
        String enchants = "None";

        if (meta != null && tool.getType() != Material.AIR) {
            enchants = meta.getEnchants().toString();

            for (Map.Entry<XEnchantment, Integer> entry : enchantsRequired.entrySet()) {

                Enchantment enchantment = entry.getKey().get();

                if (enchantment == null)
                    continue;

                if (meta.hasEnchant(enchantment) && meta.getEnchantLevel(enchantment) >= entry.getValue())
                    return true;
            }
        }

        player.sendMessage(Message.ENCHANT_REQUIRED_ERROR.get(player)
                .replace("%enchant%", requirements));
        log.fine(String.format("Player doesn't have the required enchants. Enchants: %s, required: %s", enchants, requirements));
        return false;
    }

    private String compressEnchantRequirements() {
        return enchantsRequired.entrySet().stream()
                .map(e -> String.format("%s (%d)",
                        TextUtil.capitalize(e.getKey().name().toLowerCase().replace("_", " ")), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    public boolean checkJobs(Player player) {
        if (BlockRegen.getInstance().getCompatibilityManager().getJobs().isLoaded() || jobsRequired.isEmpty()) {
            return true;
        }

        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);

        for (Map.Entry<Job, Integer> entry : jobsRequired.entrySet()) {
            if (Jobs.getPlayerManager().getJobsPlayer(player).isInJob(entry.getKey()) &&
                    jobsPlayer.getJobProgression(entry.getKey()).getLevel() >= entry.getValue()) {
                return true;
            }
        }

        String requirements = compressJobRequirements();

        player.sendMessage(Message.JOBS_REQUIRED_ERROR.get(player)
                .replace("%job%", requirements));
        log.fine(() -> String.format("Player doesn't have the required jobs. Jobs: %s, required: %s", jobsPlayer.getJobProgression().stream().map(JobProgression::toString).collect(Collectors.joining(", ")), requirements));
        return false;
    }

    private String compressJobRequirements() {
        return jobsRequired.entrySet().stream()
                .map(e -> String.format("%s (%d)", e.getKey().getName(), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    public void setToolsRequired(@Nullable String input) {
        if (Strings.isNullOrEmpty(input)) {
            return;
        }

        String[] arr = input.split(", ");

        toolsRequired.clear();
        for (String loop : arr) {
            XMaterial material = ParseUtil.parseMaterial(loop);
            if (material == null) {
                log.warning("Could not parse tool material " + loop);
                continue;
            }
            toolsRequired.add(material);
        }
    }

    public void setEnchantsRequired(@Nullable String input) {

        if (Strings.isNullOrEmpty(input))
            return;

        String[] arr = input.split(", ");

        enchantsRequired.clear();
        for (String loop : arr) {

            String enchantmentName = loop.split(";")[0];
            XEnchantment enchantment = ParseUtil.parseEnchantment(enchantmentName);
            if (enchantment == null) {
                log.warning("Could not parse enchantment " + enchantmentName + " in " + input);
                continue;
            }

            int level = 1;
            if (loop.contains(";")) {
                level = Integer.parseInt(loop.split(";")[1]);

                if (level < 0) {
                    log.warning("Could not parse an enchantment level in " + input);
                    continue;
                }
            }

            enchantsRequired.put(enchantment, level);
        }
    }

    public void setJobsRequired(@Nullable String input) {

        if (Strings.isNullOrEmpty(input))
            return;

        String[] arr = input.split(", ");

        jobsRequired.clear();
        for (String loop : arr) {
            Job job;
            int level = 1;

            if (loop.contains(";")) {
                job = Jobs.getJob(loop.split(";")[0]);
                level = Integer.parseInt(loop.split(";")[1]);
            } else {
                job = Jobs.getJob(loop);
            }

            jobsRequired.put(job, level);
        }
    }

    @Override
    public String toString() {
        return "PresetConditions{" +
                "toolsRequired=" + toolsRequired +
                ", enchantsRequired=" + enchantsRequired +
                ", jobsRequired=" + jobsRequired +
                '}';
    }
}