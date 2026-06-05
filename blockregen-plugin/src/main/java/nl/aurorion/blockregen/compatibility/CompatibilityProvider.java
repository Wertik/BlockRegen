package nl.aurorion.blockregen.compatibility;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;

// Provider for compatibility with other plugins.
@Log
public abstract class CompatibilityProvider {
    protected final BlockRegenPlugin plugin;

    @Getter
    private String[] features;

    @Getter
    private final String[] prefixes;

    public CompatibilityProvider(BlockRegenPlugin plugin) {
        this.plugin = plugin;
        this.prefixes = new String[0];
    }

    public CompatibilityProvider(BlockRegenPlugin plugin, String... prefixes) {
        this.plugin = plugin;
        this.prefixes = prefixes;
    }

    public void setFeatures(String... features) {
        this.features = features;
    }

    public void onLoad() {
        //
    }
}
