package nl.Aurorion.BlockRegen.BlockFormat;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Particles.ParticleBR;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BlockFormat {
    // Misc

    // Materials, checked on load, cannot be null when already loaded to cache
    private Material blockType;
    private Material replaceBlock;

    // Default: true
    private boolean regenerate;

    // Default: unlimited
    private int regenTimes;

    // Default: 3s
    private Amount regenDelay;

    // Default: false
    private boolean naturalBreak;

    // Default: true
    private boolean applyFortune;

    // Can be null
    private EventBR event;

    // Rewards

    // Commands
    private List<String> consoleCommands = new ArrayList<>();
    private List<String> playerCommands = new ArrayList<>();

    // Messages
    private List<String> broadcastMessage = new ArrayList<>();
    private List<String> informMessage = new ArrayList<>();

    // Legacy particle
    private String particle;

    // New particle format system
    private ParticleBR particleBR;

    // Vault money
    private Amount money;

    // Item drops
    private List<Drop> drops = new ArrayList<>();

    // Job Exp rewards
    private List<JobReward> jobRewards = new ArrayList<>();

    // On-regen rewards
    private List<String> onRegenConsoleCommands = new ArrayList<>();

    private List<String> onRegenBroadcastMessage = new ArrayList<>();
    private List<String> onRegenInformMessage = new ArrayList<>();

    // Conditions
    private List<String> toolsRequired = new ArrayList<>();
    private List<String> enchantsRequired = new ArrayList<>();

    private List<JobRequirement> jobRequirements = new ArrayList<>();

    // Permission player has to have
    private String permission;

    // Format validation for loading
    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    /**
     * A 'format' which the plugin follows when taking action
     *
     * @param blockType    type of block in string
     * @param replaceBlock type of block which to repalce with in string
     */

    public BlockFormat(String blockType, String replaceBlock) {
        try {
            this.blockType = Material.valueOf(blockType.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn("Material " + blockType + " is not valid, skipping the whole block..");
            valid = false;
            return;
        }

        try {
            this.replaceBlock = Material.valueOf(replaceBlock.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            Main.getInstance().cO.warn("Material " + replaceBlock + "is not valid, skipping the whole block..");
            valid = false;
            return;
        }

        valid = true;
    }

    //------------------------------------- Checked setters -------------------------------------

    public void setToolsRequired(List<String> toolsRequired) {

        for (String toolString : toolsRequired) {
            try {
                Material mat = Material.valueOf(toolString.toUpperCase());
            } catch (IllegalArgumentException e) {
                Main.getInstance().cO.warn("Tool material " + toolString + " is not valid, skipping..");
                continue;
            }

            this.toolsRequired.add(toolString);
        }

        this.toolsRequired = toolsRequired;
    }

    public void setEnchantsRequired(List<String> enchantsRequired) {
        List<String> finalEnchants = new ArrayList<>();
        for (String enchant : enchantsRequired) {
            try {
                if (enchant.contains(";"))
                    if (enchant.split(";")[1] == null)
                        enchant += "";
            } catch (ArrayIndexOutOfBoundsException e) {
                enchant = enchant.replace(";", "");
            }

            if (Main.getInstance().getEnchantUtil().get(enchant.split(";")[0]) == null) {
                Main.getInstance().cO.warn("Enchantment " + enchant.split(";")[0].toUpperCase() + " is not valid, skipping..");
                continue;
            }

            finalEnchants.add(enchant.toLowerCase());
        }

        this.enchantsRequired = finalEnchants;
    }

    public void setJobRequirements(List<String> jobRequirements) {
        for (String str : jobRequirements) {

            String levelStr = "";
            String jobName = "";

            if (str.contains(";")) {
                jobName = str.split(";")[0];

                if (str.split(";").length == 2)
                    levelStr = str.split(";")[1];
            }

            try {
                if (jobName.equals("") || Jobs.getJob(jobName) == null) {
                    Main.getInstance().cO.warn("Job name in job requirement for block " + blockType.toString() + " is not valid, skipping..");
                    continue;
                }
            } catch (NullPointerException e) {
                Main.getInstance().cO.warn("Attempted to add jobs requirement when Jobs are not loaded, skipping it.");
                continue;
            }

            int level;

            try {
                level = Integer.parseInt(levelStr);
            } catch (NumberFormatException e) {
                Main.getInstance().cO.warn("Job level in job requirement for block " + blockType.toString() + " is not valid, using 0..");
                level = 0;
            }

            this.jobRequirements.add(new JobRequirement(jobName, level));
        }
    }

    public void setParticle(String particle) {
        if (particle == null)
            particle = "";
        this.particle = particle.toLowerCase();
    }

    public void setDrops(List<Drop> drops) {
        if (drops == null)
            this.drops = new ArrayList<>();
        else
            this.drops = drops;
    }

    // ---------------------------------------- Block Break Actions -------------------------------------------

    /**
     * Returns whether or not conditions are met
     *
     * @param player Player to test conditions for
     * @return boolean
     */

    public boolean check(Player player) {

        //------------------------------------- Permission check -------------------------------------
        if (!permission.equals(""))
            if (!player.hasPermission(permission)) {
                player.sendMessage(Messages.get("Permission-Error").replace("%permission%", permission));
                Main.getInstance().cO.debug("Failed on permission check", player);
                return false;
            }

        Main.getInstance().cO.debug("Passed permission check", player);

        //------------------------------------- Tool check -------------------------------------

        ItemStack tool = player.getInventory().getItemInMainHand();

        if (toolsRequired != null)
            if (!toolsRequired.isEmpty()) {
                if (!toolsRequired.contains(tool.getType().toString().toUpperCase())) {
                    player.sendMessage(Messages.get("Tool-Required-Error").replace("%tool%", Utils.listToString(toolsRequired, "§f, §7", "§cNo tools set")));

                    Main.getInstance().cO.debug("Tool check failed", player);
                    return false;
                }
            }

        Main.getInstance().cO.debug("Tool check passed", player);

        //------------------------------------- Enchant check -------------------------------------

        if (!enchantsRequired.isEmpty()) {

            boolean ep = false;

            for (String enchant : enchantsRequired) {
                Enchantment enchantment = Main.getInstance().getEnchantUtil().get(enchant.split(";")[0]);

                if (tool.hasItemMeta())
                    if (tool.getItemMeta().hasEnchant(enchantment)) {
                        if (enchant.contains(";"))
                            if (tool.getItemMeta().getEnchantLevel(enchantment) >= Integer.parseInt(enchant.split(";")[1])) {
                                ep = true;
                                break;
                            } else
                                continue;

                        ep = true;
                        break;
                    }
            }

            if (!ep) {
                player.sendMessage(Messages.get("Enchant-Required-Error").replace("%enchant%", Utils.listToString(enchantsRequired, "§f, §7", "§cNo enchants set")));

                Main.getInstance().cO.debug("Enchant check failed", player);
                return false;
            }
        }

        Main.getInstance().cO.debug("Enchant check passed", player);

        //------------------------------------- Jobs -------------------------------------

        if (Main.getInstance().useJobs() && jobRequirements != null) {
            if (!jobRequirements.isEmpty()) {
                boolean jp = true;

                List<JobProgression> jobs = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

                List<String> jobNames = new ArrayList<>();
                jobs.forEach(job -> jobNames.add(job.getJob().getName().toLowerCase()));

                int i = 0;

                for (JobRequirement jobReq : jobRequirements) {
                    if (jobNames.contains(jobReq.getJob().toLowerCase())) {
                        if (jobs.get(i).getLevel() < jobReq.getLevel()) {
                            Main.getInstance().cO.debug("Missing the level, " + jobs.get(i).getLevel() + " < " + jobReq.getLevel(), player);
                            jp = false;
                        }
                    } else
                        jp = false;
                    i++;
                }

                HashMap<String, String> jobReqs = new HashMap<>();

                for (JobRequirement jobReq : jobRequirements)
                    jobReqs.put(jobReq.getJob(), String.valueOf(jobReq.getLevel()));

                if (!jp) {
                    player.sendMessage(Messages.get("Jobs-Error").replace("%jobs%", Utils.mapToString(jobReqs, "§f, §7", " ", "§4NaN")));

                    Main.getInstance().cO.debug("Jobs check failed", player);
                    return false;
                }
            }
        }

        Main.getInstance().cO.debug("Jobs check passed", player);

        Main.getInstance().cO.debug("Everything 'norminal', go on with regeneration.", player);
        return true;
    }

    /**
     * Actions after conditions are passed
     *
     * @param player  Player who mined
     * @param block   Mined block
     * @param expDrop Default experience drop
     */

    public void reward(Player player, Block block, int expDrop) {

        Location blockLocation = block.getLocation();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // How many times the block can be mined, unlimited if no limit
        // Used for placeholders
        String actualRegenTimes = "Unlimited";

        if (Utils.regenTimesBlocks.containsKey(blockLocation))
            actualRegenTimes = String.valueOf(Utils.regenTimesBlocks.get(blockLocation) - 1);
        else if (regenTimes != 0)
            actualRegenTimes = String.valueOf(regenTimes - 1);

        //------------------------------------- Command and Messages -------------------------------------

        // Parsing and firing console commands
        if (!consoleCommands.isEmpty())
            for (String command : consoleCommands) {
                // Utils.parse uses PlaceholderAPI integration, if possible
                command = Utils.parse(command, player, this, actualRegenTimes);
                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command);
            }

        // Parsing and firing player commands
        if (!playerCommands.isEmpty())
            for (String command : playerCommands) {
                command = Utils.parse(command, player, this, actualRegenTimes);
                Main.getInstance().getServer().dispatchCommand(player, command);
            }

        // Parsing and broadcasting messages to all online players
        if (!broadcastMessage.isEmpty())
            for (String line : broadcastMessage) {
                line = Utils.parseAndColor(line, player, this, actualRegenTimes);
                for (Player p : Main.getInstance().getServer().getOnlinePlayers())
                    p.sendMessage(line);
            }

        // Parsing and sending messages to player who broke the block
        if (!informMessage.isEmpty())
            for (String line : informMessage) {
                line = Utils.parseAndColor(line, player, this, actualRegenTimes);
                player.sendMessage(line);
            }

        Main.getInstance().cO.debug("Command and messages fired", player);

        //------------------------------------- Item & EXP Drops -------------------------------------

        // Go with vanilla drops?
        if (!naturalBreak) {
            // Custom drops
            if (!drops.isEmpty())
                for (Drop drop : drops) {

                    // Creates and parses ItemStack as configured in Blocklist.yml
                    ItemStack item = drop.getItemStack(player);

                    // Retrieves amount random/fixed based on configuration
                    expDrop = drop.getExpAmount().getAmount();

                    // Apply event boosters
                    if (drop.isApplyEvents())
                        if (event != null)
                            if (Utils.events.containsKey(event.getName()))
                                if (Utils.events.get(Utils.removeColors(event.getName()))) {
                                    if (event.isDoubleDrops())
                                        item.setAmount(item.getAmount() * 2);

                                    if (event.isDoubleXp())
                                        expDrop *= 2;
                                }

                    // Modify item amount based on Fortune enchantment
                    // Adds fortune generated amount to the base amount picked by block format
                    if (applyFortune)
                        item.setAmount(item.getAmount() + Utils.checkFortune(block.getType(), tool));

                    // Set texture, if the material is a player head
                    if (drop.getMaterial().equals(Material.PLAYER_HEAD)) {
                        ItemMeta savedMeta = item.getItemMeta();

                        Main.getInstance().cO.debug("Creating a player head..", player);

                        if (drop.getHeadOwner() != null) {
                            if (drop.getHeadOwner().equalsIgnoreCase("%player%")) {
                                SkullMeta meta = (SkullMeta) item.getItemMeta();

                                meta.setOwningPlayer(player);
                                item.setItemMeta(meta);
                            } else {
                                // Get head
                                if (drop.getHeadOwner().startsWith("url:"))
                                    Utils.itemWithUrl(item, drop.getHeadOwner().replace("url:", ""));
                                else if (drop.getHeadOwner().startsWith("base64:"))
                                    Utils.itemWithBase64(item, drop.getHeadOwner().replace("base64:", ""));
                                else if (drop.getHeadOwner().startsWith("uuid:"))
                                    try {
                                        Utils.itemWithUuid(item, UUID.fromString(drop.getHeadOwner().replace("uuid:", "")));
                                    } catch (IllegalArgumentException e) {
                                        Main.getInstance().cO.warn("Invalid UUID format: " + drop.getHeadOwner());
                                    }
                                else if (drop.getHeadOwner().startsWith("surl:"))
                                    Utils.itemWithUrl(item, "http://textures.minecraft.net/texture/" + drop.getHeadOwner().replace("surl:", ""));
                                else
                                    Main.getInstance().cO.warn("Invalid head-owner format.");
                            }
                        }

                        item = Utils.copyMeta(item, savedMeta);
                    }

                    // Drop Item
                    if (item.getAmount() > 0)
                        if (drop.isDropNaturally())
                            // Naturally
                            blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                        else
                            // Directly to inventory
                            player.getInventory().addItem(item);

                    // Drop Experience
                    if (expDrop > 0)
                        if (drop.isDropExpNaturally())
                            // Naturally
                            blockLocation.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);
                        else
                            // Add the exp
                            player.giveExp(expDrop);
                }
        } else {
            // Vanilla drops
            if (!block.getDrops().isEmpty())
                for (ItemStack item : block.getDrops()) {

                    // Event modifier
                    if (event != null)
                        if (Utils.events.get(Utils.removeColors(event.getName()))) {
                            if (event.isDoubleDrops())
                                item.setAmount(item.getAmount() * 2);

                            if (event.isDoubleXp())
                                expDrop *= 2;
                        }

                    // Modify item amount based on Fortune enchantment
                    // Works like Vanilla fortune
                    if (applyFortune)
                        item.setAmount(Utils.checkFortune(block.getType(), tool));

                    // Above can set to 0, we don't want that here.
                    if (item.getAmount() == 0)
                        item.setAmount(1);

                    // Drop Experience
                    if (expDrop > 0)
                        block.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);

                    // Drop Item
                    if (item.getAmount() > 0)
                        blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                }
        }

        Main.getInstance().cO.debug("Drops given", player);

        //------------------------------------- Special Event Item -------------------------------------

        if (event != null)
            if (Utils.events.get(Utils.removeColors(event.getName())) && event.getDrop() != null && event.isDropEnabled() && Main.getInstance().getRandom().nextInt(event.getDropRarity()) + 1 == 1) {
                ItemStack eventItem = event.getDrop().getItemStack(player);

                // Drop Item
                if (eventItem.getAmount() > 0)
                    if (event.getDrop().isDropNaturally())
                        // Naturally
                        blockLocation.getWorld().dropItemNaturally(blockLocation, eventItem);
                    else
                        // Directly to inventory
                        player.getInventory().addItem(eventItem);
            }

        Main.getInstance().cO.debug("Events rewarded if present", player);

        //------------------------------------- Vault money -------------------------------------

        if (money != null) {
            // Fetch amount
            int moneyToGive = money.getAmount();

            if (moneyToGive != 0 && Main.getInstance().getEconomy() != null) {
                EconomyResponse response = Main.getInstance().getEconomy().depositPlayer(player, moneyToGive);

                // Can fail, not so common
                if (response.transactionSuccess())
                    Main.getInstance().cO.debug("Gave " + moneyToGive + " to " + player.getName(), player);
                else Main.getInstance().cO.err("Could not deposit money to player's account.");
            }
        }

        //------------------------------------- Jobs exp -------------------------------------

        if (Main.getInstance().useJobs())
            if (!jobRewards.isEmpty())
                for (JobReward reward : jobRewards)
                    reward.reward(player);

        //------------------------------------- Particles -------------------------------------

        // We still support both options, three base types from older versions & new particleBR system
        if (particle != null) {
            showParticle(blockLocation);
            Main.getInstance().cO.debug("Legacy particles casted", player);
        } else if (particleBR != null) {
            particleBR.castParticles(blockLocation, player);
            Main.getInstance().cO.debug("Particles casted", player);
        } else Main.getInstance().cO.debug("No particles configured", player);

        Main.getInstance().cO.debug("Rewards done.", player);
    }

    /**
     * Shows particle based on configuration on {@link @location}
     *
     * @param location Location of the mined block
     */

    private void showParticle(Location location) {
        Main.getInstance().getParticles().check(particle, location);
    }

    /**
     * Actions fired once the block is regenerated
     *
     * @param player        Player who originally broke the block
     * @param blockLocation Location of the broken block
     */

    public void onRegen(@Nullable Player player, Location blockLocation) {

        Main.getInstance().cO.debug("On-regen actions running..", player);

        // Regen times placeholders
        String actualRegenTimes = "Unlimited";

        if (Utils.regenTimesBlocks.containsKey(blockLocation))
            actualRegenTimes = String.valueOf(Utils.regenTimesBlocks.get(blockLocation));
        else if (regenTimes != 0)
            actualRegenTimes = String.valueOf(regenTimes);

        //------------------------------------- Commands & Messages -------------------------------------

        // Console commands
        if (!onRegenConsoleCommands.isEmpty())
            for (String command : onRegenConsoleCommands) {
                if (player != null)
                    command = Utils.parse(command, player, this, actualRegenTimes);
                else
                    command = Utils.color(Utils.color(command));

                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command);
            }

        // Broadcast message to all online players
        if (!onRegenBroadcastMessage.isEmpty())
            for (String line : onRegenBroadcastMessage) {
                if (player != null)
                    line = Utils.parseAndColor(line, player, this, actualRegenTimes);
                else
                    line = Utils.color(Utils.parse(line, this, actualRegenTimes));

                for (Player p : Main.getInstance().getServer().getOnlinePlayers())
                    p.sendMessage(line);
            }

        if (player != null)
            // Inform message sent to player
            if (!onRegenInformMessage.isEmpty())
                for (String line : onRegenInformMessage)
                    player.sendMessage(Utils.parseAndColor(line, player, this, actualRegenTimes));

        Main.getInstance().cO.debug("Commands and messages fired", player);

        Main.getInstance().cO.debug("On-regen actions done.", player);
    }

    //------------------------------------- Getters & Setters -------------------------------------

    public Material getReplaceBlock() {
        return replaceBlock;
    }

    public Material getBlockType() {
        return blockType;
    }

    public Amount getRegenDelay() {
        return regenDelay;
    }

    public void setRegenDelay(Amount regenDelay) {
        this.regenDelay = regenDelay;
    }

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public void setConsoleCommands(List<String> consoleCommands) {
        this.consoleCommands = consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public void setPlayerCommands(List<String> playerCommands) {
        this.playerCommands = playerCommands;
    }

    public List<String> getToolsRequired() {
        return toolsRequired;
    }

    public List<String> getEnchantsRequired() {
        return enchantsRequired;
    }

    public Amount getMoney() {
        return money;
    }

    public void setMoney(Amount money) {
        this.money = money;
    }

    public int getRegenTimes() {
        return regenTimes;
    }

    public void setRegenTimes(int regenTimes) {
        this.regenTimes = regenTimes;
    }

    public String getParticle() {
        return particle;
    }

    public boolean isNaturalBreak() {
        return naturalBreak;
    }

    public void setNaturalBreak(boolean naturalBreak) {
        this.naturalBreak = naturalBreak;
    }

    public List<Drop> getDrops() {
        return drops;
    }

    public EventBR getEvent() {
        return event;
    }

    public void setEvent(EventBR event) {
        this.event = event;
    }

    public boolean isRegenerate() {
        return regenerate;
    }

    public void setRegenerate(boolean regenerate) {
        this.regenerate = regenerate;
    }

    public List<String> getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(List<String> broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public List<String> getInformMessage() {
        return informMessage;
    }

    public void setInformMessage(List<String> informMessage) {
        this.informMessage = informMessage;
    }

    public void setOnRegenConsoleCommands(List<String> onRegenConsoleCommands) {
        this.onRegenConsoleCommands = onRegenConsoleCommands;
    }

    public void setOnRegenBroadcastMessage(List<String> onRegenBroadcastMessage) {
        this.onRegenBroadcastMessage = onRegenBroadcastMessage;
    }

    public void setOnRegenInformMessage(List<String> onRegenInformMessage) {
        this.onRegenInformMessage = onRegenInformMessage;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public ParticleBR getParticleBR() {
        return particleBR;
    }

    public void setParticleBR(ParticleBR particleBR) {
        this.particleBR = particleBR;
    }

    public boolean isApplyFortune() {
        return applyFortune;
    }

    public void setApplyFortune(boolean applyFortune) {
        this.applyFortune = applyFortune;
    }

    public List<JobReward> getJobRewards() {
        return jobRewards;
    }

    public void setJobRewards(List<JobReward> jobRewards) {
        this.jobRewards = jobRewards;
    }
}