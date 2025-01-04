package nl.aurorion.blockregen.system.preset.drop;

import nl.aurorion.blockregen.system.drop.ItemProvider;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public class ExternalDropItem extends DropItem {

    private final String id;
    private final ItemProvider provider;

    public ExternalDropItem(ItemProvider provider, String id) {
        this.provider = provider;
        this.id = id;
    }

    @Override
    public ItemStack toItemStack(Function<String, String> parser) {
        int amount = this.amount.getInt();
        if (amount <= 0) {
            return null;
        }
        return provider.createItem(this.id, parser, amount);
    }

    @Override
    public String toString() {
        return "ExternalDropItem{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", dropNaturally=" + dropNaturally +
                ", chance=" + chance +
                ", experienceDrop=" + experienceDrop +
                '}';
    }
}
