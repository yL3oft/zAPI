package me.yleoft.zAPI.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    public static void markItem(@NotNull ItemStack item, @NotNull String mark) {
        NBT.modify(item, nbt -> {
            nbt.setBoolean(mark, true);
        });
    }

    /**
     * Unmarks an item with a specific mark.
     * @param item The item to unmark.
     * @param mark The mark to remove from the item.
     */
    public static void unmarkItem(@NotNull ItemStack item, @NotNull String mark) {
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
    public static boolean isMarked(@NotNull ItemStack item, @NotNull String mark) {
        return NBT.get(item, nbt -> {
            if(!nbt.hasTag(mark)) return false;
            return nbt.getBoolean(mark);
        });
    }

    /**
     * Adds a custom command NBT to an item.
     * @param item The item to add the command to.
     * @param command The command to add.
     */
    public static void addCustomCommand(@NotNull ItemStack item, @NotNull String command, boolean isConsole) {
        NBT.modify(item, nbt -> {
            String customCommand = command;
            if(isConsole) customCommand = "[CON]"+command;
            if(nbt.hasTag(customCommandNBT)) {
                if(nbt.getString(customCommandNBT).contains(command)) return;
                customCommand = nbt.getString(customCommandNBT)+"||"+command;
            }
            nbt.setString(customCommandNBT, customCommand);
        });
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull List<String> commands, boolean isConsole) {
        for(String command : commands) {
            addCustomCommand(item, command, isConsole);
        }
    }
    public static void addCustomCommands(@NotNull ItemStack item, @NotNull List<String> commands) {
        for(String command : commands) {
            addCustomCommand(item, command);
        }
    }
    public static void addCustomCommand(@NotNull ItemStack item, @NotNull String command) {
        addCustomCommand(item, command, false);
    }

    /**
     * Removes a custom command NBT from an item.
     * @param item The item to remove the command from.
     * @param command The command to remove.
     */
    public static void removeCustomCommand(@NotNull ItemStack item, @NotNull String command) {
        NBT.modify(item, nbt -> {
            String customCommand = nbt.getString(customCommandNBT);
            if(customCommand.equals(command)) {
                nbt.removeKey(customCommandNBT);
            }
            if(customCommand.contains("||") || customCommand.contains(command)) {
                if(customCommand.contains(command+"||")) customCommand = customCommand.replace(command+"||", "");
                if(customCommand.contains("||"+command)) customCommand = customCommand.replace("||"+command, "");
                nbt.setString(customCommandNBT, customCommand);
            }
        });
    }

    /**
     * Gets the custom commands from an item.
     * @param item The item to get the commands from.
     * @return A list of custom commands.
     */
    public static List<String> getCustomCommands(@NotNull ItemStack item) {
        return NBT.get(item, nbt -> {
            List<String> commands = new ArrayList<>();
            if(nbt.hasTag(customCommandNBT)) {
                commands = List.of(nbt.getString(customCommandNBT).split("\\|\\|"));
            }
            return commands;
        });
    }

}
