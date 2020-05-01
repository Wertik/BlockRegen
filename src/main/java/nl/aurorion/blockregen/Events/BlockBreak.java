package nl.aurorion.blockregen.Events;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.palmergames.bukkit.towny.TownyAPI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.System.Getters;
import nl.aurorion.blockregen.Utils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class BlockBreak implements Listener {

    private final BlockRegen plugin;

    public BlockBreak(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check bypass
        if (Utils.bypass.contains(player.getName()))
            return;

        // Check regen
        if (Utils.regenBlocks.contains(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        String blockName = block.getType().name();
        BlockData blockData = block.getBlockData();

        // Block data check
        if (Utils.dataCheck.contains(player.getName())) {
            event.setCancelled(true);
            player.sendMessage(Message.DATA_CHECK.get().replace("%block%", blockName));
            return;
        }

        FileConfiguration settings = plugin.getFiles().getSettings().getFileConfiguration();

        // Towny
        if (plugin.getGetters().useTowny() && plugin.getServer().getPluginManager().getPlugin("Towny") != null) {
            if (TownyAPI.getInstance().getTownBlock(block.getLocation()) != null)
                if (TownyAPI.getInstance().getTownBlock(block.getLocation()).hasTown())
                    return;
        }

        // Grief Prevention
        if (plugin.getGetters().useGriefPrevention() && plugin.getGriefPrevention() != null) {
            String noBuildReason = plugin.getGriefPrevention().allowBreak(player, block, block.getLocation(), event);
            if (noBuildReason != null) return;
        }

        // WorldGuard
        if (plugin.getGetters().useWorldGuard() && plugin.getWorldGuardProvider() != null) {
            if (!plugin.getWorldGuardProvider().canBreak(player, block.getLocation())) return;
        }

        // Residence
        if (plugin.getGetters().useResidence() && plugin.getResidence() != null) {
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(block.getLocation());

            if (residence != null) {
                ResidencePermissions permissions = residence.getPermissions();
                if (!permissions.playerHas(player, Flags.build, true)) return;
            }
        }

        FileConfiguration blockList = plugin.getFiles().getBlocklist().getFileConfiguration();

        // Check worlds
        String worldName = player.getWorld().getName();
        List<String> worlds = settings.getStringList("Worlds-Enabled");

        boolean isInWorld = false;
        for (String world : worlds) {
            if (world.equalsIgnoreCase(worldName)) {
                isInWorld = true;
                break;
            }
        }

        // Load blocks
        ConfigurationSection blockSection = blockList.getConfigurationSection("Blocks");

        List<String> blocks = blockSection == null ? new ArrayList<>() : new ArrayList<>(blockSection.getKeys(false));

        if (isInWorld) {

            World bworld = block.getWorld();

            boolean useRegions = plugin.getGetters().useRegions();

            boolean isInRegion = false;

            if (useRegions && plugin.getWorldEdit() != null) {
                ConfigurationSection regionSection = plugin.getFiles().getRegions().getFileConfiguration().getConfigurationSection("Regions");

                List<String> regions = regionSection == null ? new ArrayList<>() : new ArrayList<>(regionSection.getKeys(false));

                for (String region : regions) {
                    String max = plugin.getFiles().getRegions().getFileConfiguration().getString("Regions." + region + ".Max");
                    String min = plugin.getFiles().getRegions().getFileConfiguration().getString("Regions." + region + ".Min");

                    if (min == null || max == null)
                        continue;

                    Location locA = Utils.stringToLocation(max);
                    Location locB = Utils.stringToLocation(min);

                    CuboidRegion selection = new CuboidRegion(BukkitAdapter.asBlockVector(locA), BukkitAdapter.asBlockVector(locB));

                    if (selection.contains(BukkitAdapter.asBlockVector(block.getLocation()))) {
                        isInRegion = true;
                        break;
                    }
                }
            }

            if (blocks.contains(blockName)) {

                int expToDrop = event.getExpToDrop();

                if (isInRegion) {

                    if (!player.hasPermission("blockregen.block." + blockName) && !player.hasPermission("blockregen.block.*") && !player.isOp()) {
                        player.sendMessage(Message.PERMISSION_BLOCK_ERROR.get());
                        event.setCancelled(true);
                        return;
                    }

                    if ((plugin.getGetters().toolRequired(blockName) != null) && (!toolCheck(plugin.getGetters().toolRequired(blockName), player))) {
                        event.setCancelled(true);
                        return;
                    }

                    if ((plugin.getGetters().enchantRequired(blockName) != null) && (!enchantCheck(plugin.getGetters().enchantRequired(blockName), player))) {
                        event.setCancelled(true);
                        return;
                    }

                    if (plugin.getGetters().jobsCheck(blockName) != null) {
                        if (!jobsCheck(plugin.getGetters().jobsCheck(blockName), player)) {
                            event.setCancelled(true);
                            return;
                        }
                    }

                    event.setDropItems(false);
                    event.setExpToDrop(0);
                    this.blockBreak(player, block, blockName, blockData, bworld, expToDrop);
                } else {
                    if (!useRegions) {

                        if (!player.hasPermission("blockregen.block." + blockName) && !player.hasPermission("blockregen.block.*") && !player.isOp()) {
                            player.sendMessage(Message.PERMISSION_BLOCK_ERROR.get());
                            event.setCancelled(true);
                            return;
                        }

                        if ((plugin.getGetters().toolRequired(blockName) != null) && (!toolCheck(plugin.getGetters().toolRequired(blockName), player))) {
                            event.setCancelled(true);
                            return;
                        }

                        if ((plugin.getGetters().enchantRequired(blockName) != null) && (!enchantCheck(plugin.getGetters().enchantRequired(blockName), player))) {
                            event.setCancelled(true);
                            return;
                        }

                        if (plugin.getGetters().jobsCheck(blockName) != null) {
                            if (!jobsCheck(plugin.getGetters().jobsCheck(blockName), player)) {
                                event.setCancelled(true);
                                return;
                            }
                        }

                        event.setDropItems(false);
                        event.setExpToDrop(0);
                        this.blockBreak(player, block, blockName, blockData, bworld, expToDrop);
                    }
                }
            } else {
                if ((isInRegion && BlockRegen.getInstance().getGetters().disableOtherBreakRegion()) ||
                        BlockRegen.getInstance().getGetters().disableOtherBreak())

                    event.setCancelled(true);
            }
        }
    }

    private boolean toolCheck(String string, Player player) {
        String[] tools = string.split(", ");
        boolean check = false;
        for (String all : tools) {
            Material tool = Material.valueOf(all.toUpperCase());
            if (player.getInventory().getItemInMainHand().getType() == tool) {
                check = true;
                break;
            }
        }
        if (check) {
            return true;
        }
        player.sendMessage(Message.TOOL_REQUIRED_ERROR.get().replace("%tool%", string.toLowerCase().replace("_", " ")));
        return false;
    }

    private boolean enchantCheck(String string, Player player) {
        String[] enchants = string.split(", ");
        boolean check = false;
        for (String all : enchants) {
            Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(all.toLowerCase()));
            if (enchant == null)
                continue;

            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            if (meta != null) {
                if (meta.hasEnchant(enchant)) {
                    check = true;
                    break;
                }
            } else check = true;
        }

        if (check)
            return true;

        player.sendMessage(Message.ENCHANT_REQUIRED_ERROR.get().replace("%enchant%", string.toLowerCase().replace("_", " ")));
        return false;
    }

    private boolean jobsCheck(String string, Player player) {
        String job = null;
        int level = 0;
        String[] jobCheckString = string.split(";");
        List<JobProgression> jobs = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();
        for (JobProgression OneJob : jobs) {
            if (OneJob.getJob().equals(Jobs.getJob(jobCheckString[0]))) {
                job = OneJob.getJob().getName();
                level = OneJob.getLevel();
                break;
            }
        }
        if (job == null || !job.equals(jobCheckString[0])) {
            player.sendMessage(Message.JOBS_REQUIRED_ERROR.get().replace("%job%", jobCheckString[0]).replace("%level%", jobCheckString[1]));
            return false;
        } else if (level < Integer.parseInt(jobCheckString[1])) {
            player.sendMessage(Message.JOBS_REQUIRED_ERROR.get().replace("%job%", jobCheckString[0]).replace("%level%", jobCheckString[1]));
            return false;
        } else {
            return true;
        }
    }

    private void blockBreak(Player player, Block block, String blockName, BlockData blockData, World world, int expToDrop) {
        Getters getters = plugin.getGetters();
        BlockState state = block.getState();
        Location location = block.getLocation();

        List<ItemStack> drops = new ArrayList<>();

        // Events ---------------------------------------------------------------------------------------------
        boolean doubleDrops = false;
        boolean doubleExp = false;
        ItemStack eventItem = null;
        boolean dropEventItem = false;
        int rarity = 0;

        if (getters.eventName(blockName) != null && Utils.events.containsKey(getters.eventName(blockName)))
            if (Utils.events.get(getters.eventName(blockName))) {

                doubleDrops = getters.eventDoubleDrops(blockName);
                doubleExp = getters.eventDoubleExp(blockName);

                if (getters.eventItemMaterial(blockName) != null) {
                    int amount = getters.eventItemAmount(blockName, player);

                    if (amount > 0) {
                        eventItem = new ItemStack(getters.eventItemMaterial(blockName), amount);
                        ItemMeta meta = eventItem.getItemMeta();

                        if (meta != null) {
                            if (getters.eventItemName(blockName, player) != null)
                                meta.setDisplayName(getters.eventItemName(blockName, player));

                            if (!getters.eventItemLore(blockName, player).isEmpty())
                                meta.setLore(getters.eventItemLore(blockName, player));
                        }

                        eventItem.setItemMeta(meta);
                        dropEventItem = getters.eventItemDropNaturally(blockName);
                        rarity = getters.eventItemRarity(blockName);
                    }
                }
            }

        // Drop Section-----------------------------------------------------------------------------------------
        Material dropMaterial = getters.dropItemMaterial(blockName);

        if (getters.naturalBreak(blockName)) {
            for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
                Material mat = drop.getType();
                int amount;

                if (doubleDrops) {
                    amount = drop.getAmount() * 2;
                } else {
                    amount = drop.getAmount();
                }

                ItemStack dropItem = new ItemStack(mat, amount);

                BlockRegen.getInstance().consoleOutput.debug("Dropping item " + dropItem.getType().toString() + "x" + dropItem.getAmount());
                drops.add(dropItem);
            }

            if (expToDrop > 0) {
                if (doubleExp) {
                    world.spawn(location, ExperienceOrb.class).setExperience(expToDrop * 2);
                } else {
                    world.spawn(location, ExperienceOrb.class).setExperience(expToDrop);
                }
            }
        } else {
            if (dropMaterial != null && dropMaterial != Material.AIR) {
                int itemAmount = getters.dropItemAmount(blockName);

                if (getters.applyFortune(blockName) && itemAmount > 0)
                    itemAmount = Utils.applyFortune(block.getType(), player.getInventory().getItemInMainHand()) + itemAmount;

                if (doubleDrops) itemAmount = itemAmount * 2;

                ItemStack dropItem = new ItemStack(dropMaterial, itemAmount);
                ItemMeta dropMeta = dropItem.getItemMeta();

                if (dropMeta != null) {
                    if (getters.dropItemName(blockName, player) != null) {
                        dropMeta.setDisplayName(getters.dropItemName(blockName, player));
                    }

                    if (!getters.dropItemLore(blockName, player).isEmpty()) {
                        dropMeta.setLore(getters.dropItemLore(blockName, player));
                    }

                    dropItem.setItemMeta(dropMeta);
                }

                BlockRegen.getInstance().consoleOutput.debug("Dropping item " + dropItem.getType().toString() + "x" + dropItem.getAmount());
                if (itemAmount > 0) drops.add(dropItem);
            }

            int expAmount = getters.dropItemExpAmount(blockName);

            if (expAmount > 0) {
                if (doubleExp)
                    expAmount = expAmount * 2;

                if (getters.dropItemExpDrop(blockName)) {
                    world.spawn(location, ExperienceOrb.class).setExperience(expAmount);
                } else {
                    player.giveExp(expAmount);
                }
            }
        }

        if (eventItem != null &&
                dropEventItem &&
                (plugin.getRandom().nextInt((rarity - 1) + 1) + 1) == 1) {
            drops.add(eventItem);
        }

        for (ItemStack drop : drops) {
            if (plugin.getGetters().dropItemsNaturally(blockName))
                world.dropItemNaturally(location, drop);
            else player.getInventory().addItem(drop);
        }

        // Jobs Rewards ----------------------------------------------------------------------------------------

        if (plugin.getGetters().useJobsRewards() && plugin.getJobsProvider() != null) {
            plugin.getJobsProvider().triggerBlockBreakAction(player, block);
        }

        // Vault money -----------------------------------------------------------------------------------------
        if (plugin.getEconomy() != null) {
            int money = getters.money(blockName);
            if (money > 0)
                plugin.getEconomy().depositPlayer(player, money);
        }

        // Commands execution -----------------------------------------------------------------------------------
        if (getters.consoleCommands(blockName) != null)
            getters.consoleCommands(blockName).forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.parse(command, player)));

        if (getters.playerCommands(blockName) != null)
            getters.playerCommands(blockName).forEach(command -> Bukkit.dispatchCommand(player, Utils.parse(command, player)));

        // Particles -------------------------------------------------------------------------------------------
        if (getters.particleCheck(blockName) != null)
            plugin.getParticleUtil().run(getters.particleCheck(blockName).toLowerCase(), block);

        // Data Recovery ---------------------------------------------------------------------------------------
        FileConfiguration data = plugin.getFiles().getData().getFileConfiguration();

        if (getters.dataRecovery()) {
            List<String> dataLocs = new ArrayList<>();

            if (data.contains(blockName))
                dataLocs = data.getStringList(blockName);

            dataLocs.add(Utils.locationToString(location));
            data.set(blockName, dataLocs);
            plugin.getFiles().getData().save();
        } else
            Utils.persist.put(location, block.getType());

        Material replaceMaterial = getters.replaceBlock(blockName);

        // Replacing the block ---------------------------------------------------------------------------------
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(replaceMaterial);
                BlockRegen.getInstance().consoleOutput.debug("Replaced block with " + replaceMaterial.toString());
            }
        }.runTaskLater(plugin, 2L);

        Utils.regenBlocks.add(location);

        // Actual Regeneration -------------------------------------------------------------------------------------

        int regenDelay = getters.replaceDelay(blockName);
        regenDelay = regenDelay == 0 ? 1 : regenDelay;
        BlockRegen.getInstance().consoleOutput.debug("Regen Delay: " + regenDelay);

        Material regenerateInto = plugin.getGetters().regenBlock(blockName);

        if (regenerateInto != state.getType()) {
            state.setType(regenerateInto);
            state.setBlockData(blockData);
            plugin.getConsoleOutput().debug("Regenerate into: " + regenerateInto.toString());
        }

        BukkitTask task = new BukkitRunnable() {
            public void run() {
                regen(state, location, data, blockName);
            }
        }.runTaskLater(plugin, regenDelay * 20);

        Utils.tasks.put(location, task);
    }

    private void regen(BlockState state, Location location, FileConfiguration data, String blockName) {
        state.update(true);
        BlockRegen.getInstance().consoleOutput.debug("Regenerated block " + blockName);

        Utils.persist.remove(location);
        Utils.regenBlocks.remove(location);
        Utils.tasks.remove(location);

        if (data != null && data.contains(blockName)) {
            List<String> dataLocs = data.getStringList(blockName);

            if (!dataLocs.isEmpty()) {
                dataLocs.remove(Utils.locationToString(location));
                data.set(blockName, dataLocs);
                plugin.getFiles().getData().save();
            }
        }
    }
}
