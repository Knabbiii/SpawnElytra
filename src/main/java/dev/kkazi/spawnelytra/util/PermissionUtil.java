package dev.kkazi.spawnelytra.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnElytra");
    private static LuckPerms luckPerms;
    private static boolean luckPermsAvailable = false;

    static {
        try {
            luckPerms = LuckPermsProvider.get();
            luckPermsAvailable = true;
            LOGGER.info("SpawnElytra - LuckPerms integration enabled!");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            LOGGER.info("SpawnElytra - LuckPerms not found, using default permissions");
        }
    }

    public static boolean canUseFlight(ServerPlayer player) {
        if (!luckPermsAvailable) return true;
        User user = luckPerms.getUserManager().getUser(player.getUUID());
        if (user == null) return true;
        return user.getCachedData().getPermissionData().checkPermission("spawnelytra.use").asBoolean() || player.hasPermissions(2);
    }

    public static boolean canUseBoost(ServerPlayer player) {
        if (!luckPermsAvailable) return true;
        User user = luckPerms.getUserManager().getUser(player.getUUID());
        if (user == null) return true;
        return user.getCachedData().getPermissionData().checkPermission("spawnelytra.useboost").asBoolean() || player.hasPermissions(2);
    }

    public static boolean hasPermission(CommandSourceStack source, String permission, int level) {
        if (permission.equals("spawnelytra.admin")) {
            return source.hasPermission(2);
        }
        return source.hasPermission(level);
    }
}
