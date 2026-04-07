package me.yleoft.zAPI.command.parameter;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface Parameter {

    @NotNull String name();

    default @NotNull List<String> aliases() { return List.of(); }

    default String permission() { return null; }

    default boolean stopSubCommands() { return false; }

    default int minArgs() { return 0; }

    default int maxArgs() { return Integer.MAX_VALUE; }

    @Nullable
    default Set<String> whitelist() { return null; }

    @Nullable
    default String whitelistMessage() { return null; }

    default boolean isWhitelisted(@NotNull String value) {
        Set<String> whitelist = whitelist();
        if (whitelist == null || whitelist.isEmpty()) return true;

        Set<String> lowercaseWhitelist = whitelist.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return lowercaseWhitelist.contains(value.toLowerCase());
    }

    default void execute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] parameterArgs) {}

    default @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] parameterArgs) {
        return List.of();
    }
}