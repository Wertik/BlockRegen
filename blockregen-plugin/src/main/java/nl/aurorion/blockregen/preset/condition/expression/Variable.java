package nl.aurorion.blockregen.preset.condition.expression;

import nl.aurorion.blockregen.Context;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.util.Text;
import org.bukkit.entity.Player;

// Placeholder or another value. Gets parsed at execution time.
public class Variable implements Operand {

    @Getter
    private final String content;

    public Variable(String content) {
        this.content = content;
    }

    @Override
    public Object value(Context ctx) {
        String result = Text.parse(content, ctx.values().values().toArray());
        if (BlockRegenPlugin.getInstance().isUsePlaceholderAPI()) {
            result = PlaceholderAPI.setPlaceholders((Player) ctx.mustVar("player"), result);
        }
        return Operand.Parser.parseObject(result);
    }

    @Override
    public String toString() {
        return "Variable{" +
                "content='" + content + '\'' +
                '}';
    }
}
