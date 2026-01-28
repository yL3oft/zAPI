package me.yleoft.zAPI.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a command parameter with its properties.
 */
public interface Parameter {

    /**
     * The name of the parameter.
     *
     * @return The name of the parameter.
     */
    @NotNull String name();

    /**
     * The aliases of the parameter.
     *
     * @return The aliases of the parameter.
     */
    default @NotNull List<String> aliases() { return List.of(); }

    /**
     * The permission required to use the parameter.
     *
     * @return The permission required to use the parameter.
     */
    default String permission() { return null; }

    /**
     * Whether to stop processing sub-commands after this parameter is executed.
     *
     * @return true to stop processing sub-commands, false otherwise.
     */
    default boolean stopSubCommands() { return false; }

    /**
     * The minimum number of arguments required for this parameter.
     *
     * @return The minimum number of arguments required for this parameter.
     */
    default int minArgs() { return 0; }

    /**
     * The maximum number of arguments allowed for this parameter.
     *
     * @return The maximum number of arguments allowed for this parameter.
     */
    default int maxArgs() { return Integer.MAX_VALUE; }

    /**
     * Called when the parameter is executed.
     * @param sender The command sender
     * @param fullArgs All args (same as Bukkit gives)
     * @param parameterArgs The args already typed for THIS parameter (not including the "-name" token)
     */
    default void execute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] parameterArgs) {};

    /**
     * Called for tab completion when the user is currently typing arguments for this parameter.
     * @param sender The command sender
     * @param fullArgs All args (same as Bukkit gives)
     * @param parameterArgs The args already typed for THIS parameter (not including the "-name" token)
     */
    default @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] parameterArgs) {
        return List.of();
    }
}