package nl.aurorion.blockregen;

import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Message system, loaded on enable & reload.
 *
 * @author Wertik1206
 */
@Getter
public enum Message {

    PREFIX("Prefix"),
    UPDATE("Update"),

    /**
     * Command general messages.
     */
    NO_PERM("Insufficient-Permission"),
    ONLY_PLAYERS("Console-Sender-Error"),
    INVALID_COMMAND("Invalid-Command"),

    TOO_MANY_ARGS("Too-Many-Arguments"),
    NOT_ENOUGH_ARGS("Not-Enough-Arguments"),
    ARGUMENT_NOT_A_NUMBER("Argument-Not-A-Number"),

    RELOAD("Reload"),

    TOOLS("Tools"),
    UNKNOWN_ARGUMENT("Unknown-Argument"),

    /**
     * Bypass
     */
    BYPASS_ON("Bypass-On"),
    BYPASS_OFF("Bypass-Off"),

    /**
     * Debug
     */
    DEBUG_ON("Debug-On"),
    DEBUG_OFF("Debug-Off"),

    /**
     * Data check
     */
    DATA_CHECK("Data-Check"),
    DATA_CHECK_NODE_DATA("Data-Check-Node-Data"),
    DATA_CHECK_ON("Data-Check-On"),
    DATA_CHECK_OFF("Data-Check-Off"),

    /**
     * Regions
     */
    NO_SELECTION("No-Region-Selected"),
    DUPLICATED_REGION("Duplicated-Region"),
    DUPLICATED_WORLD_REGION("Duplicated-World-Region"),
    SET_REGION("Set-Region"),
    REGION_FROM_WORLD("Set-World-Region"),
    REGION_PRIORITY_CHANGED("Region-Priority-Changed"),
    REMOVE_REGION("Remove-Region"),
    UNKNOWN_REGION("Unknown-Region"),
    COULD_NOT_CREATE_REGION("Could-Not-Create-Region"),
    SELECT_FIRST("Select-First"),
    SELECT_SECOND("Select-Second"),
    SET_ALL("Set-All"),
    INVALID_PRESET("Invalid-Preset"),

    HAS_PRESET_ALREADY("Has-Preset-Already"),
    DOES_NOT_HAVE_PRESET("Does-Not-Have-Preset"),
    PRESET_ADDED("Preset-Added"),
    PRESET_REMOVED("Preset-Removed"),
    PRESETS_CLEARED("Presets-Cleared"),
    PRESETS_COPIED("Presets-Copied"),
    REGENERATED_PROCESSES("Regenerated-Processes"),

    /**
     * Events
     */
    ACTIVATE_EVENT("Activate-Event"),
    DEACTIVATE_EVENT("De-Activate-Event"),
    EVENT_NOT_FOUND("Event-Not-Found"),
    EVENT_ALREADY_ACTIVE("Event-Already-Active"),
    EVENT_NOT_ACTIVE("Event-Not-Active"),

    /**
     * Messages on block break errs.
     */
    TOOL_REQUIRED_ERROR("Tool-Required-Error"),
    ENCHANT_REQUIRED_ERROR("Enchant-Required-Error"),
    JOBS_REQUIRED_ERROR("Jobs-Error"),
    PERMISSION_BLOCK_ERROR("Permission-Error"),
    PERMISSION_REGION_ERROR("Permission-Region-Error");

    private static final List<String> KEYS = new ArrayList<>();

    static {
        for (Message message : values()) {
            KEYS.add(message.getKey());
        }
    }

    private final String key;

    Message(String key) {
        this.key = key;
    }

    public static List<String> keys() {
        return Collections.unmodifiableList(KEYS);
    }

    public MessageWrapper get() {
        return BlockRegen.getInstance().getLanguageManager().get(this.key);
    }

    public void send(CommandSender sender) {
        BlockRegen.getInstance().getLanguageManager().send(sender, this.key);
    }

    public String raw() {
        return BlockRegen.getInstance().getLanguageManager().raw(this.key);
    }
}
