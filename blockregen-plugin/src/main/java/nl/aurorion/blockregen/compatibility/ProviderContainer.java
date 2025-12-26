package nl.aurorion.blockregen.compatibility;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegenPlugin;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProviderContainer<T extends CompatibilityProvider> {

    private final BlockRegenPlugin plugin;

    @Getter
    private final String pluginName;

    @Getter
    @Setter
    private boolean found = false;

    // Should presets be reloaded after this plugin is found?
    @Setter
    private boolean requiresReloadAfterFound = false;

    private final Supplier<T> supplier;
    private T instance;

    public ProviderContainer(BlockRegenPlugin plugin, String pluginName, Supplier<T> provider) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.supplier = provider;
    }

    public void load() throws ProviderLoadException {
        try {
            this.instance = supplier.get();
            instance.onLoad();
        } catch (Exception e) {
            this.instance = null;
            throw new ProviderLoadException("Failed to load provider for plugin " + this.pluginName, e);
        }
    }

    public void ifLoaded(Consumer<T> consumer) {
        if (this.isLoaded()) {
            consumer.accept(this.instance);
        }
    }

    public T get() {
        if (this.instance == null) {
            load();
        }
        return this.instance;
    }

    public boolean isLoaded() {
        return this.instance != null;
    }

    public boolean requiresReloadAfterFound() {
        return this.requiresReloadAfterFound;
    }

    public ProviderContainer<T> requiresReloadAfterFound(boolean val) {
        this.requiresReloadAfterFound = val;
        return this;
    }

    public boolean isPluginEnabled() {
        return plugin.getServer().getPluginManager().isPluginEnabled(getPluginName());
    }
}
