package nl.aurorion.blockregen;

import lombok.Getter;
import nl.aurorion.blockregen.system.region.struct.RegenerationArea;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageWrapper {

    private final BlockRegen plugin;

    @Getter
    private final String originalMessage;

    @Getter
    private String message;

    public MessageWrapper(BlockRegen plugin, String originalMessage) {
        this.plugin = plugin;
        this.originalMessage = originalMessage;
        this.message = originalMessage;
    }

    private String ensureSigns(String str) {
        return str.startsWith("%") && str.endsWith("%") ? str : "%".concat(str).concat("%");
    }

    public MessageWrapper placeholder(String key, String value) {
        this.message = message.replace("(?i)" + ensureSigns(key), value);
        return this;
    }

    public MessageWrapper replace(String key, String value) {
        this.message = this.message.replace("(?i)%" + key + "%", value);
        return this;
    }

    public MessageWrapper block(Block block) {
        this.message = TextUtil.parse(this.message, block);
        return this;
    }

    public MessageWrapper player(@NotNull OfflinePlayer player) {
        this.message = TextUtil.parse(this.message, player);
        return this.placeholder("player", player.getName());
    }

    public MessageWrapper region(@Nullable RegenerationArea area) {
        if (area == null) {
            return this;
        }
        return this.placeholder("region", area.getName());
    }

    public void send(@NotNull CommandSender sender) {
        sender.sendMessage(StringUtil.color(TextUtil.parse(message)));
    }
}
