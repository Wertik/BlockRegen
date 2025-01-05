package nl.aurorion.blockregen.system.preset.drop;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.configuration.LoadResult;
import nl.aurorion.blockregen.system.preset.NumberValue;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

@Data
@NoArgsConstructor
public class ExperienceDrop {

    private boolean dropNaturally = true;

    private NumberValue amount;

    @Nullable
    public static ExperienceDrop load(@Nullable ConfigurationSection section, MinecraftDropItem itemDrop) {
        if (section == null) {
            return null;
        }

        ExperienceDrop drop = new ExperienceDrop();
        LoadResult.tryLoad(section, "amount", NumberValue.Parser::load)
                .ifNotFull(NumberValue.fixed(0))
                .apply(drop::setAmount);
        drop.setDropNaturally(section.getBoolean("drop-naturally", itemDrop.isDropNaturally()));
        return drop;
    }
}