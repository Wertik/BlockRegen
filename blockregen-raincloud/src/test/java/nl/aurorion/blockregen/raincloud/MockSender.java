package nl.aurorion.blockregen.raincloud;

import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Log
@NoArgsConstructor
public class MockSender implements CommandSender {
    @Override
    public void sendMessage(@NotNull String s) {
        log.info(s);
    }

    @Override
    public void sendMessage(@NotNull String... strings) {
        for (String s : strings) {
            log.info(s);
        }
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {

    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String... strings) {

    }

    @Override
    public @NotNull Server getServer() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return "TestSender";
    }

    @Override
    public @NotNull Spigot spigot() {
        return null;
    }

    @Override
    public boolean isPermissionSet(@NotNull String s) {
        return false;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission permission) {
        return false;
    }

    @Override
    public boolean hasPermission(@NotNull String s) {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull Permission permission) {
        return true;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String s, boolean b) {
        return null;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return null;
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String s, boolean b, int i) {
        return null;
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int i) {
        return null;
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment permissionAttachment) {

    }

    @Override
    public void recalculatePermissions() {

    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return new HashSet<>();
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public void setOp(boolean b) {

    }
}
