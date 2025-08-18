package me.yleoft.zAPI.utils;

import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.yleoft.zAPI.zAPI.isFolia;

/**
 * Utility class for player-related operations.
 */
public abstract class PlayerUtils {

    private static final Pattern START_PATTERN = Pattern.compile("^\\[(\\d+(?:\\.\\d+)?)]");

    /**
     * Retrieves an OfflinePlayer by name, using the Folia API if available.
     *
     * @param uuid the name of the player
     * @return the OfflinePlayer object, or null if not found
     */
    public static OfflinePlayer getOfflinePlayer(@NotNull UUID uuid) {
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
    public static OfflinePlayer getOfflinePlayer(@NotNull String name) {
        if(isFolia()) {
            try {
                Method getOfflinePlayerMethod = Bukkit.getServer().getClass().getMethod("getOfflinePlayerIfCached", String.class);
                return (OfflinePlayer) getOfflinePlayerMethod.invoke(Bukkit.getServer(), name);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
        }
        return Bukkit.getOfflinePlayer(name);
    }

    /**
     * Performs a custom command to the player or console.
     *
     * @param player        the player to perform the command as, or null for console
     * @param command       the command to perform
     * @param chanceParsed  whether the command has already been parsed for chance
     */
    public static void performCommand(@Nullable Player player, @NotNull String command, boolean chanceParsed) {
        if(command.startsWith("[CON]") || (!command.startsWith("[") && player == null)) {
            command = cleanCommand(command.replace("[CON]", ""));
            String finalCommand = command;
            SchedulerUtils.runTask(null, () -> zAPI.getPlugin().getServer().dispatchCommand(zAPI.getPlugin().getServer().getConsoleSender(), finalCommand));
            return;
        }
        if(command.startsWith("[INV]")) {
            if(player == null) return;
            command = cleanCommand(command.replace("[INV]", ""));
            if(command.equalsIgnoreCase("close")) player.closeInventory();
            return;
        }
        if(!chanceParsed && command.startsWith("[")) {
            Matcher matcher = START_PATTERN.matcher(command);
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                double chance = Double.parseDouble(numberStr);
                if(chance < 0 || chance > 100) {
                    zAPI.getPlugin().getLogger().warning("Invalid chance value: " + chance + " in command: " + command);
                    return;
                }
                if (Math.random() * 100 > chance) {
                    return;
                }
                command = matcher.replaceFirst("");
                performCommand(player, cleanCommand(command), true);
                return;
            }
        }
        if(player == null) return;
        @NotNull String finalCommand = command;
        SchedulerUtils.runTask(player.getLocation(), () -> player.performCommand(cleanCommand(finalCommand)));
    }
    public static void performCommand(@Nullable Player player, @NotNull String command) {
        performCommand(player, command, false);
    }
    public static void performCommand(@Nullable Player p, @NotNull List<String> commands) {
        commands.forEach(cmd -> performCommand(p, cmd, false));
    }

    private static String cleanCommand(@Nullable String command) {
        if(command == null) return "";
        int max = 0;
        while(command.startsWith(" ")) {
            command = command.substring(1);
            if(max >= 10) {
                break;
            }
            max++;
        }
        return command;
    }

}
