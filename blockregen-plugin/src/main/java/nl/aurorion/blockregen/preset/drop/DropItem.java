package nl.aurorion.blockregen.preset.drop;

import nl.aurorion.blockregen.Context;
import nl.aurorion.blockregen.conditional.Condition;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPluginImpl;
import nl.aurorion.blockregen.preset.NumberValue;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

@Getter
@Log
public abstract class DropItem {

    @Setter
    protected NumberValue amount = NumberValue.fixed(1);

    @Setter
    protected boolean dropNaturally = false;

    @Setter
    protected boolean applyFortune = true;

    @Setter
    protected NumberValue chance;

    @Setter
    protected ExperienceDrop experienceDrop;

    @Setter
    protected Condition condition;

    // Serialize this drop into an item stack.
    public abstract ItemStack toItemStack(Context context);

    public boolean shouldDrop() {
        // x/100% chance to drop
        if (chance != null) {
            double threshold = chance.getDouble();
            double roll = BlockRegenPluginImpl.getInstance().getRandom().nextDouble() * 100;

            if (roll > threshold) {
                log.fine(() -> String.format("Drop %s failed chance roll, %.2f > %.2f", this, roll, threshold));
                return false;
            }
        }
        return true;
    }
}
