package nl.aurorion.blockregen.preset.drop;

import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.drop.ItemProvider;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExternalDropItem extends DropItem {

    private final String id;
    private final ItemProvider provider;

    public ExternalDropItem(ItemProvider provider, String id) {
        this.provider = provider;
        this.id = id;
    }

    @Override
    @Nullable
    public ItemStack toItemStack(Context context) {
        int amount = this.amount.getInt();
        if (amount <= 0) {
            return null;
        }
        return provider.createItem(this.id, amount, context);
    }

    @Override
    public String toString() {
        return "ExternalDropItem{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", dropNaturally=" + dropNaturally +
                ", chance=" + chance +
                ", experienceDrop=" + experienceDrop +
                ", condition=" + condition +
                '}';
    }
}
