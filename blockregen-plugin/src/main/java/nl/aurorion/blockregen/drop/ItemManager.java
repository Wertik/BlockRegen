package nl.aurorion.blockregen.drop;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Log
public class ItemManager {
    private final BlockRegen plugin;

    private final Map<String, ItemProvider> itemProviders = new HashMap<>();

    public ItemManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void registerProvider(@NotNull String prefix, @NotNull ItemProvider itemProvider) {
        this.itemProviders.put(prefix, itemProvider);
        log.fine("Registered item provider with prefix '" + prefix + "'");
    }

    public ItemProvider getProvider(String prefix) {
        return this.itemProviders.get(prefix);
    }
}