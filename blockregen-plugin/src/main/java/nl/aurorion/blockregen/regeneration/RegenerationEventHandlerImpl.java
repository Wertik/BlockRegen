package nl.aurorion.blockregen.regeneration;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.api.BlockRegenBlockBreakEvent;
import nl.aurorion.blockregen.compatibility.provider.GriefPreventionProvider;
import nl.aurorion.blockregen.compatibility.provider.ResidenceProvider;
import nl.aurorion.blockregen.compatibility.provider.TownyProvider;
import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.event.struct.PresetEvent;
import nl.aurorion.blockregen.material.BlockRegenMaterial;
import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.preset.drop.DropItem;
import nl.aurorion.blockregen.preset.drop.ExperienceDrop;
import nl.aurorion.blockregen.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import nl.aurorion.blockregen.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Log
public class RegenerationEventHandlerImpl implements RegenerationEventHandler {

    private final BlockRegenPlugin plugin;

    public RegenerationEventHandlerImpl(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public <E extends Event> void handleEvent(Block block, Player player, E event, EventControl<E> eventControl, RegenerationEventType type) {
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
                eventControl.cancel();
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
            eventControl.cancel();
            log.fine(() -> "Player has block check.");
            return;
        }

        // If the block is protected, do nothing.
        if (checkProtection(player, block, type)) {
            return;
        }

        World world = block.getWorld();

        boolean useRegions = plugin.getConfig().getBoolean("Use-Regions", false);
        RegenerationArea area = useRegions ? plugin.getRegionManager().getArea(block) : null;

        boolean isInWorld = plugin.getConfig().getStringList("Worlds-Enabled").contains(world.getName());
        boolean isInArea = area != null;

        boolean isInZone = useRegions ? isInArea : isInWorld;

        if (!isInZone) {
            return;
        }

        // Check region permissions
        if (isInArea && Permissions.lacksPermission(player, "blockregen.region", area.getName())) {
            eventControl.cancel();
            Message.PERMISSION_REGION_ERROR.send(player);
            log.fine(() -> String.format("Player doesn't have permissions for region %s", area.getName()));
            return;
        }

        // Check block permissions
        // Mostly kept out of backwards compatibility with peoples settings and expectancies over how this works.
        if (Permissions.lacksPermission(player, "blockregen.block", block.getType().toString())) {
            eventControl.cancel();
            Message.PERMISSION_BLOCK_ERROR.send(player);
            log.fine(() -> String.format("Player doesn't have permission for block %s.", block.getType()));
            return;
        }

        log.fine(() -> String.format("Handling %s.", Locations.locationToString(block.getLocation())));

        BlockPreset preset = plugin.getPresetManager().getPreset(block, area);

        boolean isConfigured = preset != null;

        if (!isConfigured) {
            boolean disableOtherBreak;
            if (useRegions && area.getDisableOtherBreak() != null) {
                disableOtherBreak = area.getDisableOtherBreak();
            } else {
                disableOtherBreak = plugin.getConfig().getBoolean("Disable-Other-Break", false);
            }

            if (disableOtherBreak) {
                eventControl.cancel();
                log.fine(() -> String.format("%s is not a configured preset. Denied block break.", block.getType()));
                return;
            }

            log.fine(() -> String.format("%s is not a configured preset.", block.getType()));
            return;
        }

        // Check preset permissions
        if (Permissions.lacksPermission(player, "blockregen.preset", preset.getName())) {
            Message.PERMISSION_BLOCK_ERROR.send(player);
            eventControl.cancel();
            log.fine(() -> String.format("Player doesn't have permission for preset %s.", preset.getName()));
            return;
        }

        // Check conditions
        if (!preset.getConditions().check(player)) {
            eventControl.cancel();
            log.fine(() -> "Player doesn't meet conditions.");
            return;
        }

        Context ctx = Context.empty()
                .with("player", player)
                .with("tool", plugin.getVersionManager().getMethods().getItemInMainHand(player))
                .with("block", block);

        // Check advanced conditions
        try {
            if (!preset.getCondition().matches(ctx)) {
                String message = preset.getConditionMessage();
                if (message == null) {
                    message = Message.CONDITIONS_NOT_MET.getRawPrefixed();
                }
                player.sendMessage(Colors.color(Text.replace(Text.parse(message, block, player),
                        "condition", preset.getCondition().pretty())));
                eventControl.cancel();
                log.fine(() -> "Player doesn't meet conditions.");
                return;
            }
        } catch (ParseException e) {
            log.warning("Failed to run conditions for preset " + preset.getName() + ": " + e.getMessage());
            eventControl.cancel();
            return;
        }

        BlockRegenBlockBreakEvent blockRegenBlockBreakEvent = new BlockRegenBlockBreakEvent(block, preset, event, type, area);
        Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockBreakEvent);

        if (blockRegenBlockBreakEvent.isCancelled()) {
            log.fine(() -> "BlockRegenBreakEvent got cancelled.");
            return;
        }

        int vanillaExperience = eventControl.getDefaultExperience();
        eventControl.cancelDrops();

        // Multiblock vegetation - sugarcane, cacti, bamboo
        if (Blocks.isMultiblockCrop(plugin, block) && preset.isHandleCrops()) {
            handleMultiblockCrop(block, player, preset, area, vanillaExperience);
            return;
        }

        // Crop possibly above this block.
        Block above = block.getRelative(BlockFace.UP);

        Pair<String, BlockRegenMaterial> aboveResult = plugin.getMaterialManager().getMaterial(above);
        BlockRegenMaterial aboveMaterial = aboveResult == null ? null : aboveResult.getSecond();
        log.fine(() -> "Above: " + aboveMaterial);

        BlockPreset abovePreset = plugin.getPresetManager().getPreset(above, area);
        if (aboveMaterial != null && abovePreset != null && abovePreset.isHandleCrops()) {
            XMaterial aboveType = aboveMaterial.getType();

            if (Blocks.isMultiblockCrop(aboveType)) {
                // Multiblock crops (cactus, sugarcane,...)
                handleMultiblockCrop(above, player, abovePreset, area, vanillaExperience);
            } else if (XBlock.isCrop(aboveType) || Blocks.reliesOnBlockBelow(aboveType)) {
                // Single crops (wheat, carrots,...)
                log.fine(() -> "Handling block above...");

                List<ItemStack> vanillaDrops = new ArrayList<>(above.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

                RegenerationProcess process = plugin.getRegenerationManager().createProcess(above, aboveMaterial, abovePreset, area);
                process.start();

                // Note: none of the blocks seem to drop experience when broken, should be safe to assume 0
                handleRewards(above.getState(), abovePreset, player, vanillaDrops, 0);
            }
        }

        RegenerationProcess process = plugin.getRegenerationManager().createProcess(block, preset, area);
        handleBreak(process, preset, block, player, vanillaExperience);
    }

    // Check for supported protection plugins' regions and settings.
    // If any of them are protecting this block, allow them to handle this and do nothing.
    // We do this just in case some protection plugins fire after us and the event wouldn't be canceled.
    private boolean checkProtection(Player player, Block block, RegenerationEventType type) {

        Optional<TownyProvider> townyProvider = plugin.getCompatibilityManager().getTowny().get();

        // Towny
        if (plugin.getConfig().getBoolean("Towny-Support", true) && townyProvider.isPresent()) {
            if (!townyProvider.map((provider) -> provider.canBreak(block, player)).get()) {
                return true;
            }
        }

        Optional<GriefPreventionProvider> griefPreventionProvider = plugin.getCompatibilityManager().getGriefPrevention().get();

        // Grief Prevention
        if (plugin.getConfig().getBoolean("GriefPrevention-Support", true) && griefPreventionProvider.isPresent()) {
            if (!griefPreventionProvider.map(provider -> provider.canBreak(block, player)).get()) {
                return true;
            }
        }

        // WorldGuard
        if (plugin.getConfig().getBoolean("WorldGuard-Support", true)
                && plugin.getVersionManager().getWorldGuardProvider() != null) {

            if (type == RegenerationEventType.BLOCK_BREAK) {
                if (!plugin.getVersionManager().getWorldGuardProvider().canBreak(player, block.getLocation())) {
                    log.fine(() -> "Let WorldGuard handle block break.");
                    return true;
                }
            } else if (type == RegenerationEventType.TRAMPLING) {
                if (!plugin.getVersionManager().getWorldGuardProvider().canTrample(player, block.getLocation())) {
                    log.fine(() -> "Let WorldGuard handle trampling.");
                    return true;
                }
            }
        }

        Optional<ResidenceProvider> residenceProvider = plugin.getCompatibilityManager().getResidence().get();

        // Residence
        if (plugin.getConfig().getBoolean("Residence-Support", true) && residenceProvider.isPresent()) {
            if (!residenceProvider.map((provider) -> provider.canBreak(block, player, type)).get()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBypass(Player player) {
        return plugin.getRegenerationManager().hasBypass(player)
                || (plugin.getConfig().getBoolean("Bypass-In-Creative", false)
                && player.getGameMode() == GameMode.CREATIVE);
    }

    private void handleMultiblockCrop(Block block, Player player, BlockPreset preset, @Nullable RegenerationArea area, int vanillaExp) {
        boolean regenerateWhole = Blocks.shouldForceRegenerateWhole(plugin, block) || preset.isRegenerateWhole();

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
        if (BukkitVersions.isCurrentBelow("1.8", true)) {
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
        Context context = Context.empty()
                .with("player", player)
                .with("tool", plugin.getVersionManager().getMethods().getItemInMainHand(player))
                .with("block", block)
                .with("parser", parser);

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
                    log.fine(drop.getCondition() + " " + drop.getCondition().matches(context));
                    if (!drop.getCondition().matches(context) || !drop.shouldDrop()) {
                        continue;
                    }

                    ItemStack itemStack = drop.toItemStack(context);

                    if (itemStack == null) {
                        continue;
                    }

                    if (drop.isApplyFortune()) {
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
                    if (eventDrop != null && eventDrop.shouldDrop() && eventDrop.getCondition().matches(context)) {
                        ItemStack eventStack = eventDrop.toItemStack(context);

                        if (eventStack != null) {
                            drops.put(eventStack, eventDrop.isDropNaturally());
                        }
                    }

                    // Add items from presetEvent
                    for (DropItem drop : presetEvent.getRewards().getDrops()) {
                        if (!drop.shouldDrop() || !drop.getCondition().matches(context)) {
                            continue;
                        }

                        ItemStack item = drop.toItemStack(context);

                        if (item != null) {
                            drops.put(item, drop.isDropNaturally());
                        }
                    }

                    presetEvent.getRewards().give(player, parser);
                }
            }

            // Drop/give all the items & experience at once
            giveItems(drops, state, player);
            giveExp(block.getLocation(), player, experience.get(), preset.isDropNaturally(), preset.isApplyMending());

            // Trigger Jobs Break if enabled
            if (plugin.getConfig().getBoolean("Jobs-Rewards", false) && plugin.getCompatibilityManager().getJobs().isLoaded()) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getCompatibilityManager().getJobs().get()
                                .ifPresent((jobs) -> jobs.triggerBlockBreakAction(player, block))
                );
            }

            // Other rewards - commands, money etc.
            preset.getRewards().give(player, (str) -> Text.replace(Text.parse(str, player, block), "earned_experience", experience.get()));

            if (preset.getSound() != null) {
                preset.getSound().play(block.getLocation());
            }

            if (preset.getPlayerSound() != null) {
                preset.getPlayerSound().play(player);
            }

            if (preset.getParticle() != null) {
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

    /**
     * @param applyMending Whether to apply mending when {@code naturally} is false.
     */
    private void giveExp(@NotNull Location location, @NotNull Player player, int amount, boolean naturally, boolean applyMending) {
        if (amount <= 0) {
            return;
        }

        if (naturally) {
            spawnExp(location, amount);
        } else {
            if (applyMending) {
                // Simulate mending. On Spigot there's no API. 1.13+
                int remainingExperience = plugin.getVersionManager().getMethods().applyMending(player, amount);
                player.giveExp(remainingExperience);
            } else {
                player.giveExp(amount);
            }
        }
    }

    private void giveItems(Map<ItemStack, Boolean> itemStacks, BlockState blockState, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Item> items = new ArrayList<>();

            for (Map.Entry<ItemStack, Boolean> entry : itemStacks.entrySet()) {
                ItemStack item = entry.getKey();

                if (entry.getValue()) {
                    log.fine(() -> "Dropping item " + item.getType() + "x" + item.getAmount());

                    Location location = blockState.getLocation().clone().add(.5, .5, .5);
                    items.add(plugin.getVersionManager().getMethods().createDroppedItem(location, item));
                } else {
                    log.fine(() -> "Giving item " + item.getType() + "x" + item.getAmount());

                    Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                    if (!left.isEmpty()) {
                        if (plugin.getConfig().getBoolean("Drop-Items-When-Full", true)) {
                            log.fine(() -> "Inventory full. Dropping item on the ground.");

                            Message.INVENTORY_FULL_DROPPED.send(player);

                            ItemStack leftStack = left.get(left.keySet().iterator().next());
                            items.add(plugin.getVersionManager().getMethods().createDroppedItem(player.getLocation(), leftStack));
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
