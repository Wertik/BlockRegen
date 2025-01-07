package nl.aurorion.blockregen.compatibility;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.api.BlockRegenPlugin;

// Provider for compatibility with other plugins.
@Log
public abstract class CompatibilityProvider {
    protected final BlockRegenPlugin plugin;

    @Getter
    private String[] features;

    @Getter
    private String prefix;

    public CompatibilityProvider(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    public CompatibilityProvider(BlockRegenPlugin plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
    }

    public void setFeatures(String... features) {
        this.features = features;
    }

    public void onLoad() {
        //
    }
}
