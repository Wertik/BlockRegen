package nl.aurorion.blockregen.system.preset.drop;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.Amount;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

@Getter
@Log
public abstract class DropItem {

    @Setter
    protected Amount amount = new Amount(1);

    @Setter
    protected boolean dropNaturally = false;

    @Setter
    protected Amount chance;

    @Setter
    protected ExperienceDrop experienceDrop;

    // Serialize this drop into an item stack.
    public abstract ItemStack toItemStack(Function<String, String> parser);

    public boolean shouldDrop() {
        // x/100% chance to drop
        if (chance != null) {
            double threshold = chance.getDouble();
            double roll = BlockRegen.getInstance().getRandom().nextDouble() * 100;

            if (roll > threshold) {
                log.fine(() -> String.format("Drop %s failed chance roll, %.2f > %.2f", this, roll, threshold));
                return false;
            }
        }
        return true;
    }
}
