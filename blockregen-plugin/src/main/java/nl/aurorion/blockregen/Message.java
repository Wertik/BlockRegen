package nl.aurorion.blockregen;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.Colors;
import nl.aurorion.blockregen.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Message system, loaded on enable & reload.
 *
 * @author Wertik1206
 */
@Log
public enum Message {

    PREFIX("Prefix", "&6[&3BlockRegen&6] &r"),

    UPDATE("Update", "\n&7A new update was found!\n" +
            "&7Current version: &c%version%\n" +
            "&7New version: &a%newVersion%"),

    /**
     * Command general messages.
     */
    NO_PERM("Insufficient-Permission", "&cYou don't have the permissions to do this!"),
    ONLY_PLAYERS("Console-Sender-Error", "&cI'm sorry but the console can not perform this command!"),
    INVALID_COMMAND("Invalid-Command", "&cThis is not a valid command!"),
    ERROR_WHILE_RUNNING_COMMAND("Error-While-Running-Command", "&cSomething went wrong, sorry.\n" +
            "%error%\n" +
            "&7There's a detailed log in the console."),

    TOO_MANY_ARGS("Too-Many-Arguments", "&cToo many arguments.\n&7Use: &f%help%"),
    NOT_ENOUGH_ARGS("Not-Enough-Arguments", "&cNot enough arguments.\n&7Use: &f%help%"),
    ARGUMENT_NOT_A_NUMBER("Argument-Not-A-Number", "&cArgument %arg% has to be a number. &7(provided: %value%)."),

    RELOAD("Reload", "&aSuccessfully reloaded Settings.yml, Messages.yml, Blocklist.yml & re-filled the events!"),

    TOOLS("Tools", "&7Gave you the tools."),
    UNKNOWN_ARGUMENT("Unknown-Argument", "&cUnknown argument."),
    INVALID_OPTION("Invalid-Option", "&cInvalid option &f'%s'&c."),

    /**
     * Bypass
     */
    BYPASS_ON("Bypass-On", "&aBypass toggled on!"),
    BYPASS_OFF("Bypass-Off", "&cBypass toggled off!"),

    /**
     * Debug
     */
    DEBUG_ON("Debug-On", "&aYou are now listening to debug messages."),
    DEBUG_OFF("Debug-Off", "&cYou are no longer listening to debug messages."),

    /**
     * Data check
     */
    UNKNOWN_TOOL_MATERIAL("Unknown-Tool-Material", "&cMaterial of the item in your hand is not supported by this plugin."),
    UNKNOWN_MATERIAL("Unknown-Material", "&cMaterial is not supported by this plugin."),
    DATA_CHECK("Data-Check", "&eThe correct name to enter in the config is: &d%block%"),
    DATA_CHECK_NODE_DATA("Data-Check-Node-Data", "&eWith exact block data: &d%data%"),
    DATA_CHECK_ON("Data-Check-On", "&aEntered Data-Check mode!"),
    DATA_CHECK_OFF("Data-Check-Off", "&cLeft Data-Check mode!"),


    /**
     * Regions
     */
    NO_SELECTION("No-Region-Selected", "&cSelect a region with WorldEdit first."),
    DUPLICATED_REGION("Duplicated-Region", "&cThere is already a region with that name!"),
    DUPLICATED_WORLD_REGION("Duplicated-World-Region", "&cThere is already a region for this world!"),
    SET_REGION("Set-Region", "&7Region &f%region% &7successfully saved!"),
    REGION_FROM_WORLD("Set-World-Region", "&7Region &f%region% &7created for world &3%world%&7!"),
    REGION_PRIORITY_CHANGED("Region-Priority-Changed", "&7Changed priority for region &f%region% &7to &f%priority%."),
    REMOVE_REGION("Remove-Region", "&aRegion successfully deleted!"),
    UNKNOWN_REGION("Unknown-Region", "&cThere is no region with that name!"),
    COULD_NOT_CREATE_REGION("Could-Not-Create-Region", "&cCould not created a region."),
    SELECT_FIRST("Select-First", "&7Set first position to &f%x%, %y%, %z%."),
    SELECT_SECOND("Select-Second", "&7Set second position to &f%x%, %y%, %z%."),
    SET_ALL("Set-All", "&7Region set to %s &7presets."),
    SET_BREAK("Set-Break", "&7Preventing players from breaking other blocks set to %s."),
    INVALID_PRESET("Invalid-Preset", "&cPreset %preset% does not exist."),

    HAS_PRESET_ALREADY("Has-Preset-Already", "&7Region &f%region% &7has preset &f%preset% &7already."),
    DOES_NOT_HAVE_PRESET("Does-Not-Have-Preset", "&7Region &f%region% &7does not have preset &f%preset%."),
    PRESET_ADDED("Preset-Added", "&7Added preset &f%preset% &7to region &f%region%."),
    PRESET_REMOVED("Preset-Removed", "&7Removed preset &f%preset% &7from region &f%region%."),
    PRESETS_CLEARED("Presets-Cleared", "&7Presets cleared from region &f%region%."),
    PRESETS_COPIED("Presets-Copied", "&7Copied presets from &f%regionFrom% &7to &f%regionTo%."),
    REGENERATED_PROCESSES("Regenerated-Processes", "&7Regenerated &f%count% &7process(es)."),

    /**
     * Events
     */
    ACTIVATE_EVENT("Activate-Event", "&aYou activated the event: &2%event%"),
    DEACTIVATE_EVENT("De-Activate-Event", "&cYou de-activated the event: &4%event%"),
    EVENT_NOT_FOUND("Event-Not-Found", "&cThis event is not found in the system. Reminder: event names are case sensitive!"),
    EVENT_ALREADY_ACTIVE("Event-Already-Active", "&cThis event is already active!"),
    EVENT_NOT_ACTIVE("Event-Not-Active", "&cThis event is currently not active!"),

    /**
     * Messages on block break errs.
     */
    TOOL_REQUIRED_ERROR("Tool-Required-Error", "&cYou can only break this block with the following tool(s): &b%tool%&c."),
    ENCHANT_REQUIRED_ERROR("Enchant-Required-Error", "&cYour tool has to have at least one of the following enchantment(s): &b%enchant%&c."),
    JOBS_REQUIRED_ERROR("Jobs-Error", "&cYou need to reach following job levels in order to break this block: &b%job%"),
    PERMISSION_BLOCK_ERROR("Permission-Error", "&cYou don't have the permission to break this block."),
    PERMISSION_REGION_ERROR("Permission-Region-Error", "&cYou don't have the permission to break in this region."),

    CONDITIONS_NOT_MET("Conditions-Not-Met", "&cIn order to break this block you have to meet these conditions: &f%condition%&c."),

    INVENTORY_FULL_DROPPED("Inventory-Full-Dropped", "&cInventory is full! Some drops fell on the ground."),
    INVENTORY_FULL_LOST("Inventory-Full-Lost", "&cInventory is full! Some drops were lost.");

    @Getter
    private final String path;

    @Getter
    @Setter
    private String value;

    @Getter
    private static boolean insertPrefix = false;

    Message(String path, String value) {
        this.path = path;
        this.value = value;
    }

    public String getRawPrefixed() {
        return insertPrefix ? "%prefix%" + this.value : this.value;
    }

    public boolean isEmpty() {
        return this.value == null || this.value.isEmpty();
    }

    public @Nullable String get() {
        return this.isEmpty() ? null : Colors.color(Text.parse(this.getRawPrefixed()));
    }

    public @NotNull Optional<String> optional() {
        return Optional.ofNullable(this.get());
    }

    public @NotNull Optional<String> optional(@NotNull Player player) {
        return Optional.ofNullable(this.get(player));
    }

    public void mapAndSend(@NotNull CommandSender sender, @NotNull Function<String, String> mapper) {
        this.optional().map(mapper).ifPresent(s -> sender.sendMessage(Colors.color(s)));
    }

    public void mapAndSend(@NotNull Player player, @NotNull Function<String, String> mapper) {
        this.optional(player).map(mapper).ifPresent(s -> player.sendMessage(Colors.color(s)));
    }

    public @Nullable String get(@NotNull Player player) {
        return this.isEmpty() ? null : Colors.color(Text.parse(getRawPrefixed(), player));
    }

    public @Nullable String get(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return get((Player) sender);
        }
        return this.isEmpty() ? null : Colors.color(Text.parse(getRawPrefixed()));
    }

    public void send(@NotNull CommandSender target) {
        String message = this.get(target);
        if (message != null) {
            target.sendMessage(message);
        }
    }

    public static void load() {
        FileConfiguration messageSection = BlockRegenPluginImpl.getInstance().getFiles().getMessages().getFileConfiguration();

        if (!messageSection.contains("Insert-Prefix")) {
            messageSection.set("Insert-Prefix", true);
        }

        boolean shouldSave = false;

        insertPrefix = messageSection.getBoolean("Insert-Prefix", true);

        for (Message msg : values()) {
            final String path = "Messages." + msg.getPath();

            boolean set = messageSection.isSet(path);

            if (!set) {
                shouldSave = true;
                messageSection.set(path, msg.getValue());
                continue;
            }

            msg.setValue(messageSection.getString(path));
        }

        if (shouldSave) {
            BlockRegenPluginImpl.getInstance().getFiles().getMessages().save();
        }
    }
}
