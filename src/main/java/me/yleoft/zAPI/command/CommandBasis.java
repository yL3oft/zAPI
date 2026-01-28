package me.yleoft.zAPI.command;

import me.yleoft.zAPI.configuration.Messages;
import me.yleoft.zAPI.utility.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface CommandBasis extends CommandExecutor, TabCompleter {

    Map<CommandBasis, List<SubCommand>> subCommands = new HashMap<>();

    @NotNull String name();
    default String description() { return ""; }
    default List<String> aliases() { return List.of(); }
    default int minArgs() { return 0; }
    default int maxArgs() { return Integer.MAX_VALUE; }
    default boolean playerOnly() { return false; }
    default String permission() { return null; }
    default String usage(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) { return ""; }
    default String permissionMessage() { return Messages.getNoPermissionDefault(); }
    default String playerOnlyMessage() { return Messages.getOnlyPlayersDefault(); }

    default boolean prexecute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String @NotNull [] args) {
        return false;
    }

    void execute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String @NotNull [] args);

    default void dispatch(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if(playerOnly() && !isPlayer(sender)) {
            message(sender, playerOnlyMessage());
            return;
        };
        if(permission() != null && !sender.hasPermission(permission())) {
            message(sender, permissionMessage());
            return;
        }
        if(prexecute(sender, fullArgs, args)) return;
        if(minArgs() > 0 && args.length < minArgs()) {
            message(sender, usage(sender, fullArgs, args));
            return;
        };
        if(args.length > maxArgs()) {
            message(sender, usage(sender, fullArgs, args));
            return;
        };
        if(args.length > 0 && subCommands.containsKey(this)) {
            for(CommandBasis subCommand : subCommands.get(this)) {
                if(subCommand.name().equalsIgnoreCase(args[0]) || subCommand.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                    subCommand.dispatch(sender, fullArgs, subArgs);
                    return;
                }
            }
        }
        execute(sender, fullArgs, args);
    }

    default @NotNull List<String> dispatchTabComplete(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if (playerOnly() && !isPlayer(sender)) return List.of();
        if (permission() != null && !sender.hasPermission(permission())) return List.of();

        if (args.length > 1 && subCommands.containsKey(this)) {
            for (CommandBasis sc : subCommands.get(this)) {
                if (sc.name().equalsIgnoreCase(args[0])
                        || sc.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(args[0]))) {

                    if (sc.permission() != null && !sender.hasPermission(sc.permission())) {
                        return List.of();
                    }

                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    return sc.dispatchTabComplete(sender, fullArgs, subArgs);
                }
            }
        }

        // Otherwise, only use this command's completions (filtered)
        return filterCompletions(args, tabComplete(sender, fullArgs, args));
    }

    default @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        return List.of();
    }

    default boolean isPlayer(@NotNull CommandSender sender) {
        return sender instanceof Player;
    }

    default void message(CommandSender sender, @Nullable String message) {
        if(message == null || message.isEmpty()) return;
        sender.sendMessage(TextFormatter.transform(sender instanceof Player ? (Player) sender : null, message));
    }

    default void message(CommandSender sender, @Nullable Component message) {
        if(message == null) return;
        sender.sendMessage(TextFormatter.transform(sender instanceof Player ? (Player) sender : null, message));
    }

    default void addSubCommand(SubCommand subCommand) {
        subCommands.computeIfAbsent(this, k -> new ArrayList<>()).add(subCommand);
    }

    default boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        dispatch(sender, args, args);
        return true;
    }

    default List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return dispatchTabComplete(sender, args, args);
    }

    /**
     * Filters candidates by a prefix (case-insensitive). Also removes duplicates while preserving order.
     * @param args       The command arguments.
     * @param candidates The collection of candidate completions.
     * @return A list of filtered completions.
     */
    default @NotNull List<String> filterCompletions(@NotNull String[] args, @NotNull Collection<String> candidates) {
        String prefix = args.length == 0 ? "" : args[args.length - 1];
        String p = prefix.toLowerCase(Locale.ROOT);

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String c : candidates) {
            if (c == null) continue;
            if (p.isEmpty() || c.toLowerCase(Locale.ROOT).startsWith(p)) out.add(c);
        }
        return new ArrayList<>(out);
    }

}
