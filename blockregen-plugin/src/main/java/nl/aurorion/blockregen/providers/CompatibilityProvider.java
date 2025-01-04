package nl.aurorion.blockregen.providers;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;

// Provider for compatibility with other plugins.
@Log
public abstract class CompatibilityProvider {
    protected final BlockRegen plugin;

    @Getter
    private String[] features;

    @Getter
    private String prefix;

    public CompatibilityProvider(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public CompatibilityProvider(BlockRegen plugin, String prefix) {
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
