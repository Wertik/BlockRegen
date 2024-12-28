package nl.aurorion.blockregen.listeners;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.api.BlockRegenBlockBreakEvent;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.drop.ExperienceDrop;
import nl.aurorion.blockregen.system.preset.struct.drop.ItemDrop;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.util.BlockUtil;
import nl.aurorion.blockregen.util.ItemUtil;
import nl.aurorion.blockregen.util.LocationUtil;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Log
public class RegenerationListener implements Listener {

    private final BlockRegen plugin;

    public RegenerationListener(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        if (plugin.getConfig().isSet("Disable-Physics") && !plugin.getConfig().getBoolean("Disable-Physics", false)) {
            return;
        }

        Block block = event.getBlock();
        World world = block.getWorld();

        boolean useRegions = plugin.getConfig().getBoolean("Use-Regions", false);
        RegenerationArea region = plugin.getRegionManager().getArea(block);

        boolean isInWorld = plugin.getConfig().getStringList("Worlds-Enabled").contains(world.getName());
        boolean isInRegion = region != null;

        boolean isInZone = useRegions ? isInRegion : isInWorld;

        if (!isInZone) {
            return;
        }

        // Only deny physics if the update is caused by a regenerating block.
        RegenerationProcess process = plugin.getRegenerationManager().getProcess(event.getSourceBlock());
        if (process == null || !process.getPreset().isDisablePhysics()) {
            return;
        }
        event.setCancelled(true);
        log.fine(() -> event.getChangedType() + " " + BlockUtil.blockToString(event.getBlock()));
    }

    // Block trampling
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            // shouldn't happen with trampling
            return;
        }

        XMaterial xMaterial = plugin.getVersionManager().getMethods().getType(block);
        if (xMaterial != XMaterial.FARMLAND) {
            return;
        }

        Player player = event.getPlayer();
        Block cropBlock = block.getRelative(BlockFace.UP);

        handleEvent(cropBlock, player, event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        handleEvent(block, player, event);
    }

    public <E extends Cancellable> void handleEvent(Block block, Player player, E event) {
        // Check if the block is regenerating already
        RegenerationProcess existingProcess = plugin.getRegenerationManager().getProcess(block);
        if (existingProcess != null) {
            // Remove the process
            if (hasBypass(player)) {
                plugin.getRegenerationManager().removeProcess(existingProcess);
                log.fine(() -> "Removed process in bypass.");
                return;
            }

            if (existingProcess.getRegenerationTime() > System.currentTimeMillis()) {
                log.fine(() -> String.format("Block is regenerating. Process: %s", existingProcess));
                event.setCancelled(true);
                return;
            }
        }

        // Check bypass
        if (hasBypass(player)) {
            log.fine(() -> "Player has bypass.");
            return;
        }

        // Block data check
        if (plugin.getRegenerationManager().hasDataCheck(player)) {
            event.setCancelled(true);
            log.fine(() -> "Player has block check.");
            return;
        }

        // If the block is protected, do nothing.
        if (checkProtection(player, block)) {
            return;
        }

        World world = block.getWorld();

        boolean useRegions = plugin.getConfig().getBoolean("Use-Regions", false);
        RegenerationArea region = plugin.getRegionManager().getArea(block);

        boolean isInWorld = plugin.getConfig().getStringList("Worlds-Enabled").contains(world.getName());
        boolean isInRegion = region != null;

        boolean isInZone = useRegions ? isInRegion : isInWorld;

        if (!isInZone) {
            return;
        }

        log.fine(() -> String.format("Handling %s.", LocationUtil.locationToString(block.getLocation())));

        BlockPreset preset = plugin.getPresetManager().getPreset(block, region);

        boolean isConfigured = useRegions ? preset != null && region.hasPreset(preset.getName()) : preset != null;

        if (!isConfigured) {
            if (useRegions && preset != null && !region.hasPreset(preset.getName())) {
                log.fine(() -> String.format("Region %s does not have preset %s configured.", region.getName(), preset.getName()));
            }

            if (plugin.getConfig().getBoolean("Disable-Other-Break")) {
                event.setCancelled(true);
                log.fine(() -> String.format("%s is not a configured preset. Denied block break.", block.getType()));
                return;
            }

            log.fine(() -> String.format("%s is not a configured preset.", block.getType()));
            return;
        }

        // Check region permissions
        if (isInRegion && lacksPermission(player, "blockregen.region", region.getName()) && !player.isOp()) {
            event.setCancelled(true);
            Message.PERMISSION_REGION_ERROR.send(player);
            log.fine(() -> String.format("Player doesn't have permissions for region %s", region.getName()));
            return;
        }

        // Check block permissions
        // Mostly kept out of backwards compatibility with peoples settings and expectancies over how this works.
        if (lacksPermission(player, "blockregen.block", block.getType().toString()) && !player.isOp()) {
            Message.PERMISSION_BLOCK_ERROR.send(player);
            event.setCancelled(true);
            log.fine(() -> String.format("Player doesn't have permission for block %s.", block.getType()));
            return;
        }

        // Check preset permissions
        if (lacksPermission(player, "blockregen.preset", preset.getName()) && !player.isOp()) {
            Message.PERMISSION_BLOCK_ERROR.send(player);
            event.setCancelled(true);
            log.fine(() -> String.format("Player doesn't have permission for preset %s.", preset.getName()));
            return;
        }

        // Check conditions
        if (!preset.getConditions().check(player)) {
            event.setCancelled(true);
            log.fine(() -> "Player doesn't meet conditions.");
            return;
        }

        // Event API
        // todo: fire for trampling as well?
        if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent) event;

            BlockRegenBlockBreakEvent blockRegenBlockBreakEvent = new BlockRegenBlockBreakEvent(blockBreakEvent, preset);
            Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockBreakEvent);

            if (blockRegenBlockBreakEvent.isCancelled()) {
                log.fine(() -> "BlockRegenBreakEvent got cancelled.");
                return;
            }
        }

        Block above = block.getRelative(BlockFace.UP);

        log.fine(() -> "Above: " + above.getType());

        // Multiblock vegetation - sugarcane, cacti, bamboo
        // Handle those blocks as well (cancel drops, rewards, etc.), but don't start regeneration processes for those.
        // Only start a regeneration process if the bottom block is broken. (configurable)
        if (BlockUtil.isMultiblockCrop(plugin, block) && preset.isHandleCrops()) {
            handleMultiblockCrop(block, player, preset, isInRegion ? region.getName() : null);
            return;
        }

        // Crop possibly above this block.
        BlockPreset abovePreset = plugin.getPresetManager().getPreset(above);
        if (abovePreset != null && abovePreset.isHandleCrops()) {
            XMaterial aboveType = plugin.getVersionManager().getMethods().getType(above);

            if (BlockUtil.isMultiblockCrop(plugin, above)) {
                // Multiblock crops (cactus, sugarcane,...)
                handleMultiblockCrop(above, player, abovePreset, isInRegion ? region.getName() : null);
            } else if (XBlock.isCrop(aboveType) || BlockUtil.reliesOnBlockBelow(aboveType)) {
                // Single crops (wheat, carrots,...)
                List<ItemStack> vanillaDrops = new ArrayList<>(block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

                RegenerationProcess process = plugin.getRegenerationManager().createProcess(above, abovePreset, isInRegion ? region.getName() : null);
                process.start();

                // Note: none of the blocks seem to drop experience when broken, should be safe to assume 0
                log.fine(() -> "Handling block above...");
                handleRewards(above.getState(), abovePreset, player, vanillaDrops, 0);
            }
        }

        int vanillaExperience = 0;

        if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent) event;
            vanillaExperience = blockBreakEvent.getExpToDrop();
            // We're dropping the items ourselves.
            if (plugin.getVersionManager().isCurrentAbove("1.8", false)) {
                blockBreakEvent.setDropItems(false);
                log.fine(() -> "Cancelled BlockDropItemEvent");
            }
            blockBreakEvent.setExpToDrop(0);
        }

        RegenerationProcess process = plugin.getRegenerationManager().createProcess(block, preset, isInRegion ? region.getName() : null);
        handleBreak(process, preset, block, player, vanillaExperience);
    }

    // Check for supported protection plugins' regions and settings.
    // If any of them are protecting this block, allow them to handle this and do nothing.
    private boolean checkProtection(Player player, Block block) {
        // Towny
        if (plugin.getConfig().getBoolean("Towny-Support", true) &&
                plugin.getServer().getPluginManager().getPlugin("Towny") != null) {

            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());

            if (townBlock != null && townBlock.hasTown()) {
                log.fine(() -> "Let Towny handle this.");
                return true;
            }
        }

        // Grief Prevention
        if (plugin.getConfig().getBoolean("GriefPrevention-Support", true) && plugin.getGriefPrevention() != null) {
            String noBuildReason = plugin.getGriefPrevention().allowBreak(player, block, block.getLocation(), null);

            if (noBuildReason != null) {
                log.fine(() -> "Let GriefPrevention handle this.");
                return true;
            }
        }

        // WorldGuard
        if (plugin.getConfig().getBoolean("WorldGuard-Support", true)
                && plugin.getVersionManager().getWorldGuardProvider() != null) {

            if (!plugin.getVersionManager().getWorldGuardProvider().canBreak(player, block.getLocation())) {
                log.fine(() -> "Let WorldGuard handle this.");
                return true;
            }
        }

        // Residence
        if (plugin.getConfig().getBoolean("Residence-Support", true) && plugin.getResidence() != null) {
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(block.getLocation());

            if (residence != null) {
                ResidencePermissions permissions = residence.getPermissions();

                // has neither build nor destroy
                // let residence run its protection
                if (!permissions.playerHas(player, Flags.destroy, true) && !permissions.playerHas(player, Flags.build, true)) {
                    log.fine(() -> "Let Residence handle this.");
                    return true;
                }
            }
        }

        return false;
    }

    /*
     We do this our own way, because default permissions don't seem to work well with LuckPerms.
     (having a wildcard permission with default: true doesn't seem to work)

     When neither of the permissions are defined allow everything.
     Specific permission takes precedence over wildcards.
    */
    private boolean lacksPermission(Player player, String permission, String specific) {
        boolean hasAll = player.hasPermission(String.format("%s.*", permission));
        boolean allDefined = player.isPermissionSet(String.format("%s.*", permission));

        boolean hasSpecific = player.hasPermission(String.format("%s.%s", permission, specific));
        boolean specificDefined = player.isPermissionSet(String.format("%s.%s", permission, specific));

        return !((hasAll && !specificDefined) || (!allDefined && !specificDefined) || (hasSpecific && specificDefined));
    }

    private boolean hasBypass(Player player) {
        return plugin.getRegenerationManager().hasBypass(player)
                || (plugin.getConfig().getBoolean("Bypass-In-Creative", false)
                && player.getGameMode() == GameMode.CREATIVE);
    }

    private void handleMultiblockCrop(Block block, Player player, BlockPreset preset, @Nullable String region) {
        boolean regenerateWhole = preset.isRegenerateWhole();

        handleMultiblockAbove(block, player, above -> BlockUtil.isMultiblockCrop(plugin, above), (b) -> {
            BlockPreset abovePreset = plugin.getPresetManager().getPreset(b);

            if (regenerateWhole && abovePreset != null && abovePreset.isHandleCrops()) {
                RegenerationProcess process = plugin.getRegenerationManager().createProcess(b, abovePreset, region);
                process.start();
            } else {
                // Just destroy...
                b.setType(Material.AIR);
            }
        });

        Block base = findBase(block);

        log.fine(() -> "Base " + BlockUtil.blockToString(base));

        // Only start regeneration when the most bottom block is broken.
        RegenerationProcess process = null;
        if (block == base || regenerateWhole) {
            process = plugin.getRegenerationManager().createProcess(block, preset, region);
        }
        handleBreak(process, preset, block, player, 0);
    }

    private Block findBase(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);

        XMaterial belowType = plugin.getVersionManager().getMethods().getType(below);
        XMaterial type = plugin.getVersionManager().getMethods().getType(block);

        // After kelp/kelp_plant is broken, the block below gets converted from kelp_plant to kelp
        if (BlockUtil.isKelp(type)) {
            if (!BlockUtil.isKelp(belowType)) {
                return block;
            } else {
                return findBase(below);
            }
        }

        if (type != belowType) {
            return block;
        }

        return findBase(below);
    }

    private void handleMultiblockAbove(Block block, Player player, Predicate<Block> filter, Consumer<Block> startProcess) {
        Block above = block.getRelative(BlockFace.UP);

        // break the blocks manually, handle them separately.
        if (filter.test(above)) {

            // recurse from top to bottom
            handleMultiblockAbove(above, player, filter, startProcess);

            BlockPreset abovePreset = plugin.getPresetManager().getPreset(above);

            if (abovePreset != null) {
                List<ItemStack> vanillaDrops = new ArrayList<>(block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

                startProcess.accept(above);

                // Note: none of the blocks seem to drop experience when broken, should be safe to assume 0
                handleRewards(above.getState(), abovePreset, player, vanillaDrops, 0);
            }
        }
    }

    private void handleBreak(@Nullable RegenerationProcess process, BlockPreset preset, Block block, Player player, int vanillaExperience) {
        BlockState state = block.getState();

        List<ItemStack> vanillaDrops = new ArrayList<>(block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

        // Cancels item drops below 1.8.
        if (plugin.getVersionManager().isCurrentBelow("1.8", true)) {
            block.setType(Material.AIR);
        }

        // Start regeneration
        // After setting to AIR on 1.8 to prevent conflict
        if (process != null) {
            process.start();
        }

        handleRewards(state, preset, player, vanillaDrops, vanillaExperience);
    }

    private void handleRewards(BlockState state, BlockPreset preset, Player player, List<ItemStack> vanillaDrops, int vanillaExperience) {
        Block block = state.getBlock();

        Function<String, String> parser = (str) -> TextUtil.parse(str, player, block);

        // Run rewards async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<ItemStack, Boolean> drops = new HashMap<>();
            int experience = 0;

            // Items and exp
            if (preset.isNaturalBreak()) {

                for (ItemStack drop : vanillaDrops) {
                    drops.put(drop, preset.isDropNaturally());
                }

                experience += vanillaExperience;
            } else {
                for (ItemDrop drop : preset.getRewards().getDrops()) {
                    ItemStack itemStack = drop.toItemStack(player, parser);

                    if (itemStack == null) {
                        continue;
                    }

                    if (preset.isApplyFortune()) {
                        itemStack.setAmount(ItemUtil.applyFortune(block.getType(),
                                plugin.getVersionManager().getMethods().getItemInMainHand(player))
                                + itemStack.getAmount());
                    }

                    drops.put(itemStack, drop.isDropNaturally());

                    ExperienceDrop experienceDrop = drop.getExperienceDrop();
                    if (experienceDrop != null) {
                        experience += experienceDrop.getAmount().getInt();
                    }
                }
            }

            PresetEvent presetEvent = plugin.getEventManager().getEvent(preset.getName());

            // Event
            if (presetEvent != null && presetEvent.isEnabled()) {

                // Double drops and exp
                if (presetEvent.isDoubleDrops()) {
                    drops.keySet().forEach(drop -> drop.setAmount(drop.getAmount() * 2));
                }
                if (presetEvent.isDoubleExperience()) {
                    experience *= 2;
                }

                // Item reward
                if (plugin.getRandom().nextInt(presetEvent.getItemRarity().getInt()) == 0) {
                    ItemDrop eventDrop = presetEvent.getItem();

                    // Event item
                    if (eventDrop != null) {
                        ItemStack eventStack = eventDrop.toItemStack(player, parser);

                        if (eventStack != null) {
                            drops.put(eventStack, eventDrop.isDropNaturally());
                        }
                    }

                    // Add items from presetEvent
                    for (ItemDrop drop : presetEvent.getRewards().getDrops()) {
                        ItemStack item = drop.toItemStack(player, parser);

                        if (item != null) {
                            drops.put(item, drop.isDropNaturally());
                        }
                    }

                    presetEvent.getRewards().give(player, parser);
                }
            }

            // Drop/give all the items & experience at once
            giveItems(drops, state, player);
            giveExp(block.getLocation(), player, experience, preset.isDropNaturally());

            // Trigger Jobs Break if enabled
            if (plugin.getConfig().getBoolean("Jobs-Rewards", false) && plugin.getJobsProvider() != null) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getJobsProvider().triggerBlockBreakAction(player, block));
            }

            // Other rewards - commands, money etc.
            preset.getRewards().give(player, (str) -> TextUtil.parse(str, player, block));

            if (preset.getSound() != null) {
                preset.getSound().play(block.getLocation());
            }

            if (preset.getParticle() != null && plugin.getVersionManager().isCurrentAbove("1.8", false)) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getParticleManager().displayParticle(preset.getParticle(), block));
            }
        });
    }

    private void spawnExp(Location location, int amount) {
        if (location.getWorld() == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                () -> location.getWorld().spawn(location, ExperienceOrb.class).setExperience(amount));
        log.fine(() -> String.format("Spawning xp (%d).", amount));
    }

    private void giveExp(Location location, Player player, int amount, boolean naturally) {

        if (amount == 0) {
            return;
        }

        if (naturally) {
            spawnExp(location, amount);
        } else {
            player.giveExp(amount);
        }
    }

    private void giveItems(Map<ItemStack, Boolean> itemStacks, BlockState blockState, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Item> items = new ArrayList<>();

            for (Map.Entry<ItemStack, Boolean> entry : itemStacks.entrySet()) {
                ItemStack item = entry.getKey();

                if (entry.getValue()) {
                    items.add(blockState.getWorld().dropItemNaturally(blockState.getLocation(), item));
                    log.fine(() -> "Dropping item " + item.getType() + "x" + item.getAmount());
                } else {
                    player.getInventory().addItem(item);
                    log.fine(() -> "Giving item " + item.getType() + "x" + item.getAmount());
                }
            }

            plugin.getVersionManager().getMethods().handleDropItemEvent(player, blockState, items);
        });
    }
}
