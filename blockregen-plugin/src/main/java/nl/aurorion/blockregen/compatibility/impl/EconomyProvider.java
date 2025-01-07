package nl.aurorion.blockregen.compatibility.impl;

import lombok.Getter;
import lombok.extern.java.Log;
import net.milkbowl.vault.economy.Economy;
import nl.aurorion.blockregen.api.BlockRegenPlugin;
import nl.aurorion.blockregen.compatibility.CompatibilityProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

@Log
public class EconomyProvider extends CompatibilityProvider {

    @Getter
    private Economy economy;

    public EconomyProvider(BlockRegenPlugin plugin) {
        super(plugin);
        setFeatures("rewards");
    }

    public void depositPlayer(OfflinePlayer player, double money) {
        this.economy.depositPlayer(player, money);
    }

    @Override
    public void onLoad() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Found Vault, but no Economy Provider is registered.");
        }
        economy = rsp.getProvider();
    }
}
