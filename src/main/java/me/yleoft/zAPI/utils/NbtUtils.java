package me.yleoft.zAPI.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static me.yleoft.zAPI.zAPI.customCommandNBT;

/**
 * NbtUtils class to handle NBT operations on items.
 */
public abstract class NbtUtils {

    /**
     * Marks an item with a specific mark.
     * @param item The item to mark.
     * @param mark The mark to apply to the item.
     */
    public static void markItem(@NotNull ItemStack item, @NotNull final String mark) {
        NBT.modify(item, nbt -> {
            nbt.setBoolean(mark, true);
        });
    }

    /**
     * Unmarks an item with a specific mark.
     * @param item The item to unmark.
     * @param mark The mark to remove from the item.
     */
    public static void unmarkItem(@NotNull ItemStack item, @NotNull final String mark) {
        NBT.modify(item, nbt -> {
            nbt.setBoolean(mark, false);
        });
    }

    /**
     * Checks if an item is marked with a specific mark.
     * @param item The item to check.
     * @param mark The mark to check for.
     * @return True if the item is marked, false otherwise.
     */
    public static boolean isMarked(@NotNull final ItemStack item, @NotNull final String mark) {
        return NBT.get(item, nbt -> {
            if (!nbt.hasTag(mark)) return false;
            return nbt.getBoolean(mark);
        });
    }

    /**
     * Adds a custom command NBT to an item.
     * @param item The item to add the command to.
     * @param command The command to add.
     */
    public static void addCustomCommand(@NotNull ItemStack item, @NotNull final String command, @Nullable final HashMap<String, String> replaces, boolean isConsole) {
        NBT.modify(item, nbt -> {
            String customCommand = command;
            if(replaces != null) {
                for(String key : replaces.keySet()) {
                    customCommand = customCommand.replace(key, replaces.get(key));
                }
            }
            if(isConsole) customCommand = "[CON]" + customCommand;
            if(nbt.hasTag(customCommandNBT)) {
                if(nbt.getString(customCommandNBT).contains(customCommand)) return;
                customCommand = nbt.getString(customCommandNBT) + "||" + customCommand;
            }
            nbt.setString(customCommandNBT, customCommand);
        });
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull final List<String> commands, @Nullable final HashMap<String, String> replaces, boolean isConsole) {
        for(String command : commands) {
            addCustomCommand(item, command, replaces, isConsole);
        }
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull final List<String> commands, boolean isConsole) {
        for(String command : commands) {
            addCustomCommand(item, command, null, isConsole);
        }
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull final List<String> commands) {
        for(String command : commands) {
            addCustomCommand(item, command);
        }
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull final List<String> commands, @Nullable final HashMap<String, String> replaces) {
        for(String command : commands) {
            addCustomCommand(item, command, replaces);
        }
    }
    public static void addCustomCommand(@NotNull ItemStack item, @NotNull final String command, @Nullable final HashMap<String, String> replaces) {
        addCustomCommand(item, command, replaces, false);
    }
    public static void addCustomCommand(@NotNull ItemStack item, @NotNull final String command) {
        addCustomCommand(item, command, null);
    }

    /**
     * Removes a custom command NBT from an item.
     * @param item The item to remove the command from.
     * @param command The command to remove.
     */
    public static void removeCustomCommand(@NotNull ItemStack item, @NotNull final String command) {
        NBT.modify(item, nbt -> {
            String customCommand = nbt.getString(customCommandNBT);
            if(customCommand.equals(command)) {
                nbt.removeKey(customCommandNBT);
            }
            if(customCommand.contains("||") || customCommand.contains(command)) {
                if(customCommand.contains(command + "||")) customCommand = customCommand.replace(command + "||", "");
                if(customCommand.contains("||" + command)) customCommand = customCommand.replace("||" + command, "");
                nbt.setString(customCommandNBT, customCommand);
            }
        });
    }

    public static void removeCustomCommands(@NotNull ItemStack item) {
        NBT.modify(item, nbt -> {
            nbt.removeKey(customCommandNBT);
        });
    }

    /**
     * Gets the custom commands from an item.
     * @param item The item to get the commands from.
     * @return A list of custom commands.
     */
    public static List<String> getCustomCommands(@NotNull final ItemStack item) {
        return NBT.get(item, nbt -> {
            List<String> commands = new ArrayList<>();
            if(nbt.hasTag(customCommandNBT)) {
                commands = new ArrayList<>(Arrays.asList(nbt.getString(customCommandNBT).split("\\|\\|")));
            }
            return commands;
        });
    }

}