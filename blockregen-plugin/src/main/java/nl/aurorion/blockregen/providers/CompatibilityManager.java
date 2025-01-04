package nl.aurorion.blockregen.providers;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.providers.impl.*;
import nl.aurorion.blockregen.system.drop.ItemProvider;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// todo: what if a providing plugin is unloaded?
@Log
public class CompatibilityManager {
    private final BlockRegen plugin;

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
    private final ProviderContainer<GriefPreventionProvider> griefPrevention;
    @Getter
    private final ProviderContainer<ResidenceProvider> residence;
    @Getter
    private final ProviderContainer<EconomyProvider> economy;

    public CompatibilityManager(BlockRegen plugin) {
        this.plugin = plugin;

        this.jobs = createProvider("Jobs", () -> new JobsProvider(plugin))
                .requiresReloadAfterFound(true);

        this.oraxen = createProvider("Oraxen", () -> new OraxenProvider(plugin));
        this.itemsAdder = createProvider("ItemsAdder", () -> new ItemsAdderProvider(plugin));
        this.nexo = createProvider("Nexo", () -> new NexoProvider(plugin));
        this.mmoItems = createProvider("MMOItems", () -> new MMOItemsProvider(plugin));

        this.griefPrevention = createProvider("GriefPrevention", () -> new GriefPreventionProvider(plugin));
        this.residence = createProvider("Residence", () -> new ResidenceProvider(plugin));

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
            } catch (IllegalStateException e) {
                log.warning("Failed to load support for " + container.getPluginName() + ": " + e.getMessage());
                continue;
            }

            if (container.requiresReloadAfterFound()) {
                reloadPresets = true;
            }

            CompatibilityProvider provider = container.get();

            // Register parsers
            if (provider instanceof MaterialParser && provider.getPrefix() != null) {
                plugin.getMaterialManager().registerParser(provider.getPrefix(), (MaterialParser) provider);
                reloadPresets = true;
            }

            // Register ItemProviders
            if (provider instanceof ItemProvider && provider.getPrefix() != null) {
                plugin.getItemManager().registerProvider(provider.getPrefix(), (ItemProvider) provider);
                reloadPresets = true;
            }
            log.info("Loaded support for " + container.getPluginName() + "!" + (provider.getFeatures() == null ? "" : " Features: &a" + String.join("&7, &a", provider.getFeatures()) + "&7."));
        }

        if (reloadPresets && shouldReloadPresets) {
            plugin.getPresetManager().load();
            log.info("Reloading presets due to newly discovered supported plugins...");
        }
    }
}
