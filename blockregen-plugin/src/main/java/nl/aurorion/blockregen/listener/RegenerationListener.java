package nl.aurorion.blockregen.listener;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenBlockBreakEvent;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.conditional.ConditionContext;
import nl.aurorion.blockregen.event.struct.PresetEvent;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.preset.drop.DropItem;
import nl.aurorion.blockregen.preset.drop.ExperienceDrop;
import nl.aurorion.blockregen.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import nl.aurorion.blockregen.util.Blocks;
import nl.aurorion.blockregen.util.Items;
import nl.aurorion.blockregen.util.Locations;
import nl.aurorion.blockregen.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Log
public class RegenerationListener implements Listener {

    private final BlockRegenPlugin plugin;

    public RegenerationListener(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    // Block trampling
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            // shouldn't happen with trampling
            return;
        }

        XMaterial xMaterial = plugin.getBlockType(block);
        if (xMaterial != XMaterial.FARMLAND) {
            return;
        }

        Player player = event.getPlayer();
        Block cropBlock = block.getRelative(BlockFace.UP);

        handleEvent(cropBlock, player, event, EventType.TRAMPLING);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        handleEvent(block, player, event, EventType.BLOCK_BREAK);
    }

    private <E extends Cancellable> void handleEvent(Block block, Player player, E event, EventType type) {
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
        if (checkProtection(player, block, type)) {
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

        log.fine(() -> String.format("Handling %s.", Locations.locationToString(block.getLocation())));

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

        ConditionContext ctx = ConditionContext.empty()
                .with("player", player)
                .with("tool", plugin.getVersionManager().getMethods().getItemInMainHand(player))
                .with("block", block);

        // Check advanced conditions
        try {
            if (!preset.getCondition().matches(ctx)) {
                event.setCancelled(true);
                log.fine(() -> "Player doesn't meet conditions.");
                return;
            }
        } catch (ParseException e) {
            log.warning("Failed to run conditions for preset " + preset.getName() + ": " + e.getMessage());
            event.setCancelled(true);
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

        int vanillaExperience = 0;

        if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent) event;

            // We're dropping the items ourselves.
            if (plugin.getVersionManager().isCurrentAbove("1.8", false)) {
                blockBreakEvent.setDropItems(false);
                log.fine(() -> "Cancelled BlockDropItemEvent");
            }

            vanillaExperience = blockBreakEvent.getExpToDrop();
            blockBreakEvent.setExpToDrop(0);
        }

        // Multiblock vegetation - sugarcane, cacti, bamboo
        if (Blocks.isMultiblockCrop(plugin, block) && preset.isHandleCrops()) {
            handleMultiblockCrop(block, player, preset, region, vanillaExperience);
            return;
        }

        Block above = block.getRelative(BlockFace.UP);
        log.fine(() -> "Above: " + above.getType());

        // Crop possibly above this block.
        BlockPreset abovePreset = plugin.getPresetManager().getPreset(above, region);
        if (abovePreset != null && abovePreset.isHandleCrops()) {
            XMaterial aboveType = plugin.getBlockType(above);

            if (aboveType != null) {
                if (Blocks.isMultiblockCrop(aboveType)) {
                    // Multiblock crops (cactus, sugarcane,...)
                    handleMultiblockCrop(above, player, abovePreset, region, vanillaExperience);
                } else if (XBlock.isCrop(aboveType) || Blocks.reliesOnBlockBelow(aboveType)) {
                    // Single crops (wheat, carrots,...)
                    log.fine(() -> "Handling block above...");

                    List<ItemStack> vanillaDrops = new ArrayList<>(above.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

                    RegenerationProcess process = plugin.getRegenerationManager().createProcess(above, abovePreset, region);
                    process.start();

                    // Note: none of the blocks seem to drop experience when broken, should be safe to assume 0
                    handleRewards(above.getState(), abovePreset, player, vanillaDrops, 0);
                }
            }
        }

        RegenerationProcess process = plugin.getRegenerationManager().createProcess(block, preset, region);
        handleBreak(process, preset, block, player, vanillaExperience);
    }

    // Check for supported protection plugins' regions and settings.
    // If any of them are protecting this block, allow them to handle this and do nothing.
    // We do this just in case some protection plugins fire after us and the event wouldn't be cancelled.
    private boolean checkProtection(Player player, Block block, EventType type) {
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
        if (plugin.getConfig().getBoolean("GriefPrevention-Support", true) && plugin.getCompatibilityManager().getGriefPrevention().isLoaded()) {
            plugin.getCompatibilityManager().getGriefPrevention().get().canBreak(block, player);
        }

        // WorldGuard
        if (plugin.getConfig().getBoolean("WorldGuard-Support", true)
                && plugin.getVersionManager().getWorldGuardProvider() != null) {

            if (type == EventType.BLOCK_BREAK) {
                if (!plugin.getVersionManager().getWorldGuardProvider().canBreak(player, block.getLocation())) {
                    log.fine(() -> "Let WorldGuard handle block break.");
                    return true;
                }
            } else if (type == EventType.TRAMPLING) {
                if (!plugin.getVersionManager().getWorldGuardProvider().canTrample(player, block.getLocation())) {
                    log.fine(() -> "Let WorldGuard handle trampling.");
                    return true;
                }
            }
        }

        // Residence
        if (plugin.getConfig().getBoolean("Residence-Support", true) && plugin.getCompatibilityManager().getResidence().isLoaded()) {
            plugin.getCompatibilityManager().getResidence().get().canBreak(block, player, type);
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
        boolean hasAll = player.hasPermission(permission + ".*");
        boolean allDefined = player.isPermissionSet(permission + ".*");

        boolean hasSpecific = player.hasPermission(permission + "." + specific);
        boolean specificDefined = player.isPermissionSet(permission + "." + specific);

        return !((hasAll && !specificDefined) || (!allDefined && !specificDefined) || (hasSpecific && specificDefined));
    }

    private boolean hasBypass(Player player) {
        return plugin.getRegenerationManager().hasBypass(player)
                || (plugin.getConfig().getBoolean("Bypass-In-Creative", false)
                && player.getGameMode() == GameMode.CREATIVE);
    }

    private void handleMultiblockCrop(Block block, Player player, BlockPreset preset, @Nullable RegenerationArea area, int vanillaExp) {
        boolean regenerateWhole = preset.isRegenerateWhole();

        handleMultiblockAbove(block, player, above -> Blocks.isMultiblockCrop(plugin, above), (b, abovePreset) -> {
            if (regenerateWhole && abovePreset != null && abovePreset.isHandleCrops()) {
                RegenerationProcess process = plugin.getRegenerationManager().createProcess(b, abovePreset, area);
                process.start();
            } else {
                // Just destroy...
                b.setType(Material.AIR);
            }
        }, area);

        Block base;
        try {
            base = findBase(block);
        } catch (IllegalArgumentException e) {
            // invalid material
            log.fine(() -> "handleMultiBlockCrop: " + e.getMessage());
            if (!plugin.getConfig().getBoolean("Ignore-Unknown-Materials", false)) {
                throw e;
            }
            return;
        }

        log.fine(() -> "Base " + Blocks.blockToString(base));

        // Only start regeneration when the most bottom block is broken.
        RegenerationProcess process = null;
        if (block == base || regenerateWhole) {
            process = plugin.getRegenerationManager().createProcess(block, preset, area);
        }
        handleBreak(process, preset, block, player, vanillaExp);
    }

    private Block findBase(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);

        XMaterial belowType = plugin.getVersionManager().getMethods().getType(below);
        XMaterial type = plugin.getVersionManager().getMethods().getType(block);

        // After kelp/kelp_plant is broken, the block below gets converted from kelp_plant to kelp
        if (Blocks.isKelp(type)) {
            if (!Blocks.isKelp(belowType)) {
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

    private void handleMultiblockAbove(Block block, Player player, Predicate<Block> filter, BiConsumer<Block, BlockPreset> startProcess, RegenerationArea area) {
        Block above = block.getRelative(BlockFace.UP);

        // break the blocks manually, handle them separately.
        if (filter.test(above)) {

            // recurse from top to bottom
            handleMultiblockAbove(above, player, filter, startProcess, area);

            BlockPreset abovePreset = plugin.getPresetManager().getPreset(above, area);

            if (abovePreset != null) {
                List<ItemStack> vanillaDrops = new ArrayList<>(block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

                // Needs to be started here due to replacement.
                startProcess.accept(above, abovePreset);

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

        Function<String, String> parser = (str) -> Text.parse(str, player, block);

        // Conditions
        ConditionContext ctx = ConditionContext.empty()
                .with("player", player)
                .with("tool", plugin.getVersionManager().getMethods().getItemInMainHand(player))
                .with("block", block);

        // Run rewards async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<ItemStack, Boolean> drops = new HashMap<>();
            AtomicInteger experience = new AtomicInteger(0);

            // Items and exp
            if (preset.isNaturalBreak()) {

                for (ItemStack drop : vanillaDrops) {
                    drops.put(drop, preset.isDropNaturally());
                }

                experience.addAndGet(vanillaExperience);
            } else {
                for (DropItem drop : preset.getRewards().getDrops()) {
                    log.fine(drop.getCondition() + " " + drop.getCondition().matches(ctx));
                    if (!drop.getCondition().matches(ctx) || !drop.shouldDrop()) {
                        continue;
                    }

                    ItemStack itemStack = drop.toItemStack(parser);

                    if (itemStack == null) {
                        continue;
                    }

                    if (preset.isApplyFortune()) {
                        itemStack.setAmount(Items.applyFortune(block.getType(),
                                plugin.getVersionManager().getMethods().getItemInMainHand(player))
                                + itemStack.getAmount());
                    }

                    drops.put(itemStack, drop.isDropNaturally());

                    ExperienceDrop experienceDrop = drop.getExperienceDrop();
                    if (experienceDrop != null) {
                        experience.addAndGet(experienceDrop.getAmount().getInt());
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
                    experience.set(experience.get() * 2);
                }

                // Item reward
                if (plugin.getRandom().nextInt(presetEvent.getItemRarity().getInt()) == 0) {
                    DropItem eventDrop = presetEvent.getItem();

                    // Event item
                    if (eventDrop != null && eventDrop.shouldDrop() && eventDrop.getCondition().matches(ctx)) {
                        ItemStack eventStack = eventDrop.toItemStack(parser);

                        if (eventStack != null) {
                            drops.put(eventStack, eventDrop.isDropNaturally());
                        }
                    }

                    // Add items from presetEvent
                    for (DropItem drop : presetEvent.getRewards().getDrops()) {
                        if (!drop.shouldDrop() || !drop.getCondition().matches(ctx)) {
                            continue;
                        }

                        ItemStack item = drop.toItemStack(parser);

                        if (item != null) {
                            drops.put(item, drop.isDropNaturally());
                        }
                    }

                    presetEvent.getRewards().give(player, parser);
                }
            }

            // Drop/give all the items & experience at once
            giveItems(drops, state, player);
            giveExp(block.getLocation(), player, experience.get(), preset.isDropNaturally());

            // Trigger Jobs Break if enabled
            if (plugin.getConfig().getBoolean("Jobs-Rewards", false) && plugin.getCompatibilityManager().getJobs().isLoaded()) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getCompatibilityManager().getJobs().get().triggerBlockBreakAction(player, block));
            }

            // Other rewards - commands, money etc.
            preset.getRewards().give(player, (str) -> Text.replace(Text.parse(str, player, block), "earned_experience", experience.get()));

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
                    log.fine(() -> "Dropping item " + item.getType() + "x" + item.getAmount());
                    items.add(blockState.getWorld().dropItemNaturally(blockState.getLocation().clone().add(.5, .5, .5), item));
                } else {
                    log.fine(() -> "Giving item " + item.getType() + "x" + item.getAmount());

                    Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                    if (!left.isEmpty()) {
                        if (plugin.getConfig().getBoolean("Drop-Items-When-Full", true)) {
                            log.fine(() -> "Inventory full. Dropping item on the ground.");

                            Message.INVENTORY_FULL_DROPPED.send(player);

                            ItemStack leftStack = left.get(left.keySet().iterator().next());
                            items.add(player.getWorld().dropItemNaturally(player.getLocation(), leftStack));
                        } else {
                            Message.INVENTORY_FULL_LOST.send(player);
                        }
                    }
                }
            }

            plugin.getVersionManager().getMethods().handleDropItemEvent(player, blockState, items);
        });
    }
}
