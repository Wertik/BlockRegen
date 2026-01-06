package nl.aurorion.blockregen.compatibility;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.provider.*;
import nl.aurorion.blockregen.drop.ItemProvider;
import nl.aurorion.blockregen.material.MaterialProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;

@Log
public class CompatibilityManager {
    private final BlockRegenPlugin plugin;

    private final List<ProviderContainer<?>> containers = new ArrayList<>();

    @Getter
    private final ProviderContainer<JobsProvider> jobs;

    @Getter
    private final ProviderContainer<OraxenProvider> oraxen;
    @Getter
    private final ProviderContainer<ItemsAdderProvider> itemsAdder;
    @Getter
    private final ProviderContainer<NexoProvider> nexo;
    @Getter
    private final ProviderContainer<MMOItemsProvider> mmoItems;
    @Getter
    private final ProviderContainer<MythicMobsProvider> mythicMobs;

    @Getter
    private final ProviderContainer<GriefPreventionProvider> griefPrevention;
    @Getter
    private final ProviderContainer<ResidenceProvider> residence;
    @Getter
    private final ProviderContainer<TownyProvider> towny;

    @Getter
    private final ProviderContainer<EconomyProvider> economy;

    public CompatibilityManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;

        this.jobs = createProvider("Jobs", () -> new JobsProvider(plugin))
                .requiresReloadAfterFound(true);

        this.oraxen = createProvider("Oraxen", () -> new OraxenProvider(plugin));
        this.itemsAdder = createProvider("ItemsAdder", () -> new ItemsAdderProvider(plugin));
        this.nexo = createProvider("Nexo", () -> new NexoProvider(plugin));
        this.mmoItems = createProvider("MMOItems", () -> new MMOItemsProvider(plugin));
        this.mythicMobs = createProvider("MythicMobs", () -> new MythicMobsProvider(plugin));

        this.griefPrevention = createProvider("GriefPrevention", () -> new GriefPreventionProvider(plugin));
        this.residence = createProvider("Residence", () -> new ResidenceProvider(plugin));
        this.towny = createProvider("Towny", () -> new TownyProvider(plugin));

        this.economy = createProvider("Vault", () -> new EconomyProvider(plugin));
    }

    private <T extends CompatibilityProvider> ProviderContainer<T> createProvider(String pluginName, Supplier<T> supplier) {
        ProviderContainer<T> container = new ProviderContainer<>(plugin, pluginName, supplier);
        this.containers.add(container);
        return container;
    }

    // Attempt to load providers that haven't been found before.
    // For initial load we don't reload presets. They will be loaded after providers.
    public void discover(boolean shouldReloadPresets) {
        boolean reloadPresets = false;

        for (ProviderContainer<?> container : containers) {
            if (container.isFound()) {
                continue;
            }

            if (!container.isPluginEnabled()) {
                continue;
            }

            container.setFound(true);
            try {
                container.load();
            } catch (ProviderLoadException e) {
                log.log(Level.WARNING, "Failed to load provider for " + container.getPluginName() + ": " + e.getMessage(), e);
                continue;
            }

            if (container.requiresReloadAfterFound()) {
                reloadPresets = true;
            }

            Optional<? extends CompatibilityProvider> provider = container.get();

            if (!provider.isPresent()) {
                log.log(Level.WARNING, "Failed to load provider for " + container.getPluginName());
                continue;
            }

            CompatibilityProvider compatibilityProvider = provider.get();

            if (compatibilityProvider instanceof MaterialProvider && compatibilityProvider.getPrefix() != null) {
                plugin.getMaterialManager().register(compatibilityProvider.getPrefix(), (MaterialProvider) compatibilityProvider);
                reloadPresets = true;
            }

            if (compatibilityProvider instanceof ItemProvider && compatibilityProvider.getPrefix() != null) {
                plugin.getItemManager().registerProvider(compatibilityProvider.getPrefix(), (ItemProvider) compatibilityProvider);
                reloadPresets = true;
            }
            log.info("Loaded support for " + container.getPluginName() + "!" + (compatibilityProvider.getFeatures() == null ? "" : " Features: &a" + String.join("&7, &a", compatibilityProvider.getFeatures()) + "&7."));
        }

        if (reloadPresets && shouldReloadPresets) {
            log.info("Reloading presets due to newly discovered supported plugins...");
            plugin.getPresetManager().initialLoad();
        }
    }
}
