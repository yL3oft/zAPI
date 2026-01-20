package me.yleoft.zAPI.player;

import me.yleoft.zAPI.item.NbtHandler;
import me.yleoft.zAPI.utility.Version;
import me.yleoft.zAPI.utility.scheduler.Scheduler;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.yleoft.zAPI.item.NbtHandler.mark;

/**
 * Utility class for player-related operations.
 */
public final class PlayerHandler {

    private static final Pattern START_PATTERN = Pattern.compile("^\\[(\\d+(?:\\.\\d+)?)]");

    /**
     * Retrieves an OfflinePlayer by name, using the Folia API if available.
     *
     * @param uuid the name of the player
     * @return the OfflinePlayer object, or null if not found
     */
    public static OfflinePlayer getOfflinePlayer(@NotNull UUID uuid) {
        if(Version.USING_FOLIA) {
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
        if(Version.USING_FOLIA) {
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
    public static void performCommand(@Nullable Player player, @Nullable ItemStack item, @NotNull String command, boolean chanceParsed) {
        if(command.startsWith("[CON]") || (!command.startsWith("[") && player == null)) {
            command = cleanCommand(command.replace("[CON]", ""));
            String finalCommand = command;
            Scheduler.runTask(null, () -> zAPI.getPlugin().getServer().dispatchCommand(zAPI.getPlugin().getServer().getConsoleSender(), finalCommand));
            return;
        }
        if(command.startsWith("[INV]")) {
            if(player == null) return;
            command = cleanCommand(command.replace("[INV]", ""));
            if(command.equalsIgnoreCase("close")) player.closeInventory();
            return;
        }
        if(command.startsWith("[ITEM]")) {
            if(player == null || item == null) return;
            item = item.clone();

            command = cleanCommand(command.replace("[ITEM]", ""));
            if(command.equalsIgnoreCase("give") && player.getInventory().firstEmpty() != -1) {
                if(zAPI.useNBTAPI && NbtHandler.isMarked(item, mark)) {
                    NbtHandler.unmarkItem(item, mark);
                    NbtHandler.removeCustomCommands(item);
                }
                player.getInventory().addItem(item);
            }
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
                performCommand(player, item, cleanCommand(command), true);
                return;
            }
        }
        if(player == null) return;
        @NotNull String finalCommand = command;
        Scheduler.runTask(player.getLocation(), () -> player.performCommand(cleanCommand(finalCommand)));
    }
    public static void performCommand(@Nullable Player player, @Nullable ItemStack item, @NotNull String command) {
        performCommand(player, item, command, false);
    }
    public static void performCommand(@Nullable Player p, @Nullable ItemStack item, @NotNull List<String> commands) {
        commands.forEach(cmd -> performCommand(p, item, cmd));
    }
    public static void performCommand(@Nullable Player player, @NotNull String command) {
        performCommand(player, null, command);
    }
    public static void performCommand(@Nullable Player player, @NotNull List<String> commands) {
        performCommand(player, null, commands);
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
