package nl.aurorion.blockregen.util;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Permissions {
    /**
     * We do this our own way, because default permissions don't seem to work well with LuckPerms.
     * (having a wildcard permission with default: true doesn't seem to work)
     * <p>
     * When neither of the permissions are defined allow everything.
     * Specific permission takes precedence over wildcards.
     * <p>
     * OP never lacks.
     *
     * @return true if the player lacks permission (does not have it).
     */
    public static boolean lacksPermission(@NotNull CommandSender sender, @NotNull String permission, @NotNull String specific) {
        if (sender.isOp()) {
            return false;
        }

        boolean hasAll = sender.hasPermission(permission + ".*");
        boolean allDefined = sender.isPermissionSet(permission + ".*");

        boolean hasSpecific = sender.hasPermission(permission + "." + specific);
        boolean specificDefined = sender.isPermissionSet(permission + "." + specific);

        return !((hasAll && !specificDefined) || (!allDefined && !specificDefined) || (hasSpecific && specificDefined));
    }

    public static boolean hasAny(@NotNull CommandSender sender, @NotNull String[] permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
