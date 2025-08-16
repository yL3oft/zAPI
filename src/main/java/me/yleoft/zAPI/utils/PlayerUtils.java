package me.yleoft.zAPI.utils;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static me.yleoft.zAPI.zAPI.isFolia;

/**
 * Utility class for player-related operations.
 */
public abstract class PlayerUtils {

    /**
     * Retrieves an OfflinePlayer by name, using the Folia API if available.
     *
     * @param name the name of the player
     * @return the OfflinePlayer object, or null if not found
     */
    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        if(isFolia()) {
            try {
                Method getOfflinePlayerMethod = Bukkit.getServer().getClass().getMethod("getOfflinePlayerIfCached", UUID.class);
                return (OfflinePlayer) getOfflinePlayerMethod.invoke(Bukkit.getServer(), uuid);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
        }
        return Bukkit.getOfflinePlayer(uuid);
    }

    /**
     * Retrieves an OfflinePlayer by name, using the Folia API if available.
     *
     * @param name the name of the player
     * @return the OfflinePlayer object, or null if not found
     */
    public static OfflinePlayer getOfflinePlayer(String name) {
        if(isFolia()) {
            try {
                Method getOfflinePlayerMethod = Bukkit.getServer().getClass().getMethod("getOfflinePlayerIfCached", String.class);
                return (OfflinePlayer) getOfflinePlayerMethod.invoke(Bukkit.getServer(), name);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
        }
        return Bukkit.getOfflinePlayer(name);
    }

}
