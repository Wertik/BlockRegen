package nl.aurorion.blockregen.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Permissions {
    /**
     We do this our own way, because default permissions don't seem to work well with LuckPerms.
     (having a wildcard permission with default: true doesn't seem to work)

     When neither of the permissions are defined allow everything.
     Specific permission takes precedence over wildcards.

     OP never lacks.

     @return true if the player lacks permission (does not have it).
    */
    public static boolean lacksPermission(@NotNull Player player, @NotNull String permission, @NotNull String specific) {
        if (player.isOp()) {
            return false;
        }

        boolean hasAll = player.hasPermission(permission + ".*");
        boolean allDefined = player.isPermissionSet(permission + ".*");

        boolean hasSpecific = player.hasPermission(permission + "." + specific);
        boolean specificDefined = player.isPermissionSet(permission + "." + specific);

        return !((hasAll && !specificDefined) || (!allDefined && !specificDefined) || (hasSpecific && specificDefined));
    }
}
