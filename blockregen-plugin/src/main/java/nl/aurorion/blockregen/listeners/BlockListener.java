package nl.aurorion.blockregen.listeners;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
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
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.util.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Log
public class BlockListener implements Listener {

    private final BlockRegen plugin;

    public BlockListener(BlockRegen plugin) {
        this.plugin = plugin;
    }

    private boolean hasBypass(@Nullable Player player) {
        return player != null && (plugin.getRegenerationManager().hasBypass(player)
                || (plugin.getConfig().getBoolean("Bypass-In-Creative", false)
                && player.getGameMode() == GameMode.CREATIVE));
    }

    private boolean disableOtherBreak() {
        return plugin.getConfig().getBoolean("Disable-Other-Break") || plugin.getConfig().getBoolean("Disable-Other-Break-Regions");
    }

    public static class HandleResult {

        private Block block;
        private Player player;
        private BlockPreset preset;
        private RegenerationRegion region;
        private String world;
        private RegenerationProcess process;
        private Event event;
        private int expToDrop = 0;
        private boolean cancelEvent = false;
        private boolean handle = false;

        public HandleResult() {
        }

        public Block getBlock() {
            return block;
        }

        public void setBlock(Block block) {
            this.block = block;
        }

        public BlockPreset getPreset() {
            return preset;
        }

        public void setPreset(BlockPreset preset) {
            this.preset = preset;
        }

        public RegenerationProcess getProcess() {
            return process;
        }

        public void setProcess(RegenerationProcess process) {
            this.process = process;
        }

        public Event getEvent() {
            return event;
        }

        public void setEvent(Event event) {
            this.event = event;
        }

        public int getExpToDrop() {
            return expToDrop;
        }

        public void setExpToDrop(int expToDrop) {
            this.expToDrop = expToDrop;
        }

        public boolean isCancelEvent() {
            return cancelEvent;
        }

        public void setCancelEvent(boolean cancelEvent) {
            this.cancelEvent = cancelEvent;
        }

        public Player getPlayer() {
            return player;
        }

        public void setPlayer(Player player) {
            this.player = player;
        }

        public boolean isHandle() {
            return handle;
        }

        public void setHandle(boolean handle) {
            this.handle = handle;
        }

        public RegenerationRegion getRegion() {
            return region;
        }

        public void setRegion(RegenerationRegion region) {
            this.region = region;
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }
    }

    // TNT Explosions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Don't break
        Set<Block> protect = new HashSet<>();

        // Break, full drops - outside of region
        Set<Block> outside = new HashSet<>();

        // Break, no drops - these are handled by #process()
        Set<Block> handled = new HashSet<>();

        for (Block block : event.blockList()) {
            // Check if we're handling the block.

            HandleResult result = new HandleResult();
            result.setBlock(block);
            result.setEvent(event);

            handleBlock(result, block, null);

            // Cancel if wanted
            if (result.isCancelEvent()) {
                protect.add(block);
                continue;
            }

            // Check if we should handle this further.
            if (!result.isHandle()) {
                outside.add(block);
                continue;
            }

            handled.add(block);

            // Process rewards

            process(result);
        }

        // Set yield to 0, so we prevent drops from dropping
        // Only do this when we have to deny any drops.
        if (!handled.isEmpty()) {
            event.setYield(0.0f);
            log.fine("Yield set to 0.0");
        }

        // Remove blocks so they don't explode
        protect.forEach(b -> event.blockList().remove(b));

        // Break blocks outside the region.
        for (Block b : outside) {
            b.breakNaturally();
        }
    }

    // Creeper explosions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Set<Block> toRemove = new HashSet<>();

        for (Block block : event.blockList()) {
            // Check if we're handling the block.

            HandleResult result = new HandleResult();
            result.setBlock(block);
            result.setEvent(event);

            handleBlock(result, block, null);

            // Cancel if wanted
            if (result.isCancelEvent()) {
                toRemove.add(block);
                continue;
            }

            // Check if we should handle this further.
            if (!result.isHandle()) {
                continue;
            }

            // Process rewards

            process(result);
        }

        // Remove blocks that were cancelled
        toRemove.forEach(b -> event.blockList().remove(b));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {

        // Respect cancels on higher priorities
        if (event.isCancelled()) {
            log.fine("Event already cancelled.");
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if we're handling the block.

        HandleResult result = new HandleResult();

        result.setPlayer(player);
        result.setBlock(block);
        result.setEvent(event);

        // Check other plugins that are strictly for BlockBreakEvent.

        // Grief Prevention
        if (plugin.getConfig().getBoolean("GriefPrevention-Support", true) && plugin.getGriefPrevention() != null) {
            String noBuildReason = plugin.getGriefPrevention().allowBreak(player, block, block.getLocation(), event);

            if (noBuildReason != null) {
                log.fine("Let GriefPrevention handle this.");
                return;
            }
        }

        // WorldGuard
        if (plugin.getConfig().getBoolean("WorldGuard-Support", true)
                && plugin.getVersionManager().getWorldGuardProvider() != null) {

            if (!plugin.getVersionManager().getWorldGuardProvider().canBreak(player, block.getLocation())) {
                log.fine("Let WorldGuard handle this.");
                return;
            }
        }

        // Residence
        if (plugin.getConfig().getBoolean("Residence-Support", true) && plugin.getResidence() != null) {
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(block.getLocation());

            if (residence != null) {
                ResidencePermissions permissions = residence.getPermissions();

                if (!permissions.playerHas(player, Flags.build, true)) {
                    log.fine("Let Residence handle this.");
                    return;
                }
            }
        }

        handleBlock(result, block, player);

        // Cancel if wanted
        if (result.isCancelEvent()) {
            event.setCancelled(true);
            return;
        }

        // Check if we should handle this further.
        if (!result.isHandle()) {
            return;
        }

        if (result.getPreset() != null) {
            BlockRegenBlockBreakEvent blockRegenBlockBreakEvent = new BlockRegenBlockBreakEvent(event, result.getPreset());
            Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockBreakEvent);

            if (blockRegenBlockBreakEvent.isCancelled()) {
                log.fine("BlockRegenBreakEvent got cancelled.");
                return;
            }
        }

        // Disable exp
        // Remember how much exp to drop.

        result.setExpToDrop(event.getExpToDrop());

        if (plugin.getVersionManager().isCurrentAbove("1.8", false)) {
            event.setDropItems(false);
        }

        event.setExpToDrop(0);

        // Process rewards

        process(result);
    }

    private void handleBlock(@NotNull HandleResult result, @NotNull Block block, @Nullable Player player) {

        boolean useRegions = plugin.getConfig().getBoolean("Use-Regions", false);

        BlockPreset preset;

        RegenerationRegion region = plugin.getRegionManager().getRegion(block.getLocation());

        if (useRegions && region != null) {
            preset = plugin.getPresetManager().getPreset(block, region);
        } else {
            preset = plugin.getPresetManager().getPreset(block);
        }

        result.setPreset(preset);

        if (preset != null && player != null) {
            // Check permissions
            if (!player.hasPermission("blockregen.block." + preset.getName()) &&
                    !player.hasPermission("blockregen.block.*") &&
                    !player.isOp()) {
                Message.PERMISSION_BLOCK_ERROR.send(player);
                log.fine("Player doesn't have permissions.");

                result.setCancelEvent(true);
                return;
            }

            // Check conditions
            if (!preset.getConditions().check(player)) {
                log.info("Player doesn't meet conditions.");
                result.setCancelEvent(true);
                return;
            }
        }

        RegenerationProcess process = plugin.getRegenerationManager().getProcess(block);

        // Check if the block is regenerating already
        if (process != null) {

            // Remove the process
            if (hasBypass(player)) {
                plugin.getRegenerationManager().removeProcess(process);
                log.fine("Removed process in bypass.");
                return;
            }

            if (process.getRegenerationTime() > System.currentTimeMillis()) {
                log.fine(String.format("Block is regenerating. Process: %s", process));
                result.setCancelEvent(true);
                return;
            }
        }

        // Check bypass
        if (hasBypass(player)) {
            log.fine("Player has bypass.");
            return;
        }

        // Block data check
        if (player != null && plugin.getRegenerationManager().hasDataCheck(player)) {
            log.fine("Player has block check.");
            result.setCancelEvent(true);
            return;
        }

        // Towny
        if (plugin.getConfig().getBoolean("Towny-Support", true)
                && plugin.getServer().getPluginManager().getPlugin("Towny") != null) {

            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());

            if (townBlock != null && townBlock.hasTown()) {
                log.fine("Let Towny handle this.");
                return;
            }
        }

        World world = block.getWorld();

        boolean isInWorld = plugin.getConfig().getStringList("Worlds-Enabled").contains(world.getName());

        if (preset != null) {
            result.setProcess(plugin.getRegenerationManager().createProcess(block, preset));
        }

        // Check if we're in region/world.
        if (useRegions) {
            if (region == null) {
                log.fine("Not in region.");
                return;
            }

            result.setRegion(region);

            if (result.getProcess() != null) {
                result.getProcess().setRegionName(region.getName());
            }
        } else {
            if (!isInWorld) {
                log.fine(String.format("Not in world. World: %s, enabled: %s", world.getName(),
                        plugin.getConfig().getStringList("Worlds-Enabled")));
                return;
            }

            result.setWorld(world.getName());
        }

        // If there's a valid preset.
        if (preset == null) {

            // Disable block break if configured.
            if (disableOtherBreak()) {
                log.fine("Not a valid preset. Denied BlockBreak.");
                result.setCancelEvent(true);
                return;
            }

            log.fine("Not a valid preset.");
        } else {
            result.setHandle(true);
        }
    }

    private void process(@NotNull HandleResult result) {
        RegenerationProcess process = result.getProcess();
        BlockPreset preset = result.getPreset();

        Block block = result.getBlock();
        @Nullable Player player = result.getPlayer();

        List<ItemStack> vanillaDrops = player != null ? new ArrayList<>(
                block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player))) : new ArrayList<>(block.getDrops());

        if (plugin.getVersionManager().isCurrentBelow("1.8", true)) {
            block.setType(Material.AIR);
        }

        // Start regeneration
        process.start();

        // Run rewards async
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {

            // Events
            // ---------------------------------------------------------------------------------------------

            boolean doubleDrops = false;
            boolean doubleExp = false;

            PresetEvent presetEvent = plugin.getEventManager().getEvent(preset.getName());

            if (presetEvent != null && presetEvent.isEnabled()) {
                doubleDrops = presetEvent.isDoubleDrops();
                doubleExp = presetEvent.isDoubleExperience();
            }

            // Drop Section
            // -----------------------------------------------------------------------------------------
            if (preset.isNaturalBreak()) {

                for (ItemStack drop : vanillaDrops) {
                    XMaterial mat = XMaterial.matchXMaterial(drop);
                    int amount = drop.getAmount();

                    ItemStack item = mat.parseItem();

                    if (item == null) {
                        log.severe(String.format("Material %s not supported on this version.", mat));
                        continue;
                    }

                    item.setAmount(doubleDrops ? amount * 2 : amount);

                    giveItem(item, player, block, preset.isDropNaturally());
                }

                int expToDrop = result.getExpToDrop();

                if (expToDrop > 0) {
                    giveExp(block.getLocation(), player, doubleExp ? expToDrop * 2 : expToDrop, preset.isDropNaturally());
                }
            } else {
                for (ItemDrop drop : preset.getRewards().getDrops()) {
                    ItemStack itemStack = drop.toItemStack(player);

                    if (itemStack == null)
                        continue;

                    if (player != null && preset.isApplyFortune()) {
                        itemStack.setAmount(ItemUtil.applyFortune(block.getType(),
                                plugin.getVersionManager().getMethods().getItemInMainHand(player))
                                + itemStack.getAmount());
                    }

                    if (doubleDrops)
                        itemStack.setAmount(itemStack.getAmount() * 2);

                    // Drop/Give the item.

                    giveItem(itemStack, player, block, drop.isDropNaturally());

                    if (drop.getExperienceDrop() == null)
                        continue;

                    ExperienceDrop experienceDrop = drop.getExperienceDrop();

                    AtomicInteger expAmount = new AtomicInteger(experienceDrop.getAmount().getInt());

                    if (expAmount.get() <= 0)
                        continue;

                    if (doubleExp)
                        expAmount.set(expAmount.get() * 2);

                    // Drop/Give the exp.

                    giveExp(block.getLocation(), player, expAmount.get(), experienceDrop.isDropNaturally());
                }
            }

            if (presetEvent != null && presetEvent.isEnabled()) {

                // Fire rewards
                if (plugin.getRandom().nextInt(presetEvent.getItemRarity().getInt()) == 0) {

                    // Event item
                    if (presetEvent.getItem() != null) {
                        ItemDrop eventDrop = presetEvent.getItem();

                        if (eventDrop != null) {
                            ItemStack eventStack = eventDrop.toItemStack(player);
                            giveItem(eventStack, player, block, eventDrop.isDropNaturally());
                        }
                    }

                    // Add items from presetEvent
                    for (ItemDrop drop : presetEvent.getRewards().getDrops()) {
                        ItemStack item = drop.toItemStack(player);
                        giveItem(item, player, block, drop.isDropNaturally());
                    }

                    presetEvent.getRewards().give(player);
                }
            }

            // Trigger Jobs Break if enabled
            // -----------------------------------------------------------------------
            if (plugin.getConfig().getBoolean("Jobs-Rewards", false) && plugin.getJobsProvider() != null)
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getJobsProvider().triggerBlockBreakAction(player, block));

            // Rewards
            // ---------------------------------------------------------------------------------------------
            preset.getRewards().give(player);

            // Block Break Sound
            // ---------------------------------------------------------------------------------------------
            if (preset.getSound() != null)
                preset.getSound().play(block.getLocation());

            // Particles
            // -------------------------------------------------------------------------------------------
            // TODO: Make particles work on 1.8 with it's effect API.
            if (preset.getParticle() != null && plugin.getVersionManager().isCurrentAbove("1.8", false))
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getParticleManager().displayParticle(preset.getParticle(), block));
        }, 20L);
    }

    private void spawnExp(@NotNull Location location, int amount) {
        if (location.getWorld() == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                () -> location.getWorld().spawn(location, ExperienceOrb.class).setExperience(amount));
        log.fine(String.format("Spawning xp (%d).", amount));
    }

    private void giveExp(@NotNull Location location, @Nullable Player player, int amount, boolean naturally) {
        if (naturally) {
            spawnExp(location, amount);
        } else if (player != null) {
            player.giveExp(amount);
        }
    }

    private void giveItem(@Nullable ItemStack item, @Nullable Player player, @NotNull Block block, boolean naturally) {
        if (item == null) {
            return;
        }

        if (naturally) {
            dropItem(item, block);
        } else if (player != null) {
            giveItem(item, player);
        }
    }

    private void dropItem(ItemStack item, @NotNull Block block) {
        Bukkit.getScheduler().runTask(plugin, () -> block.getWorld().dropItemNaturally(block.getLocation(), item));
        log.fine("Dropping item " + item.getType() + "x" + item.getAmount());
    }

    private void giveItem(ItemStack item, @NotNull Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().addItem(item));
        log.fine("Giving item " + item.getType() + "x" + item.getAmount());
    }
}
