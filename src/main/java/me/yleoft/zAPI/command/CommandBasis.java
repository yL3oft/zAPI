package me.yleoft.zAPI.command;

import me.yleoft.zAPI.command.parameter.Parameter;
import me.yleoft.zAPI.command.parameter.ParameterParseResult;
import me.yleoft.zAPI.command.parameter.ParameterTabContext;
import me.yleoft.zAPI.configuration.Messages;
import me.yleoft.zAPI.util.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface CommandBasis extends CommandExecutor, TabCompleter {

    Map<CommandBasis, List<SubCommand>> subCommands   = new HashMap<>();
    Map<CommandBasis, List<Parameter>>  parameters     = new HashMap<>();
    Map<UUID, Map<Parameter, String[]>> activeParameters = new HashMap<>();

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

    default void addParameter(@NotNull Parameter parameter) {
        parameters.computeIfAbsent(this, k -> new ArrayList<>()).add(parameter);
    }

    default @Nullable String[] getParameter(@NotNull CommandSender sender, @NotNull Parameter parameter) {
        if (!(sender instanceof Player p)) return null;
        Map<Parameter, String[]> map = activeParameters.get(p.getUniqueId());
        return map == null ? null : map.get(parameter);
    }

    default void clearParameters(@NotNull CommandSender sender) {
        if (sender instanceof Player p) activeParameters.remove(p.getUniqueId());
    }

    default void dispatch(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if (playerOnly() && !isPlayer(sender)) { message(sender, playerOnlyMessage()); return; }
        if (permission() != null && !sender.hasPermission(permission())) { message(sender, permissionMessage()); return; }

        ParameterParseResult parsed = parseAndExecuteParameters(sender, fullArgs, args);
        if (parsed.shouldStopFurtherDispatch()) return;
        args = parsed.getRemainingArgs();

        if (prexecute(sender, fullArgs, args)) return;

        if (minArgs() > 0 && args.length < minArgs()) { message(sender, usage(sender, fullArgs, args)); return; }
        if (args.length > maxArgs())                   { message(sender, usage(sender, fullArgs, args)); return; }

        if (args.length > 0 && subCommands.containsKey(this)) {
            final String first = args[0];
            for (CommandBasis subCommand : subCommands.get(this)) {
                boolean matches = subCommand.name().equalsIgnoreCase(first)
                        || subCommand.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(first));
                if (matches) {
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                    subCommand.dispatch(sender, fullArgs, subArgs);
                    return;
                }
            }
        }

        execute(sender, fullArgs, args);
    }

    default @NotNull ParameterParseResult parseAndExecuteParameters(
            @NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        List<Parameter> list = parameters.getOrDefault(this, List.of());
        if (list.isEmpty() || args.length == 0) return new ParameterParseResult(args, false);

        boolean stop = false;
        Set<Parameter> alreadyUsed = new HashSet<>();
        List<String> remaining = new ArrayList<>(args.length);

        int i = 0;
        while (i < args.length) {
            String token = args[i];
            if (token == null || token.isEmpty() || token.charAt(0) != '-') { remaining.add(token); i++; continue; }

            String rawName = token.substring(1);
            Parameter param = findParameter(list, rawName);
            if (param == null)                 { remaining.add(token); i++; continue; }
            if (alreadyUsed.contains(param))   { remaining.add(token); i++; continue; }
            if (param.permission() != null && !sender.hasPermission(param.permission())) { remaining.add(token); i++; continue; }

            int start   = i + 1;
            int maxTake = Math.min(param.maxArgs(), args.length - start);
            int taken   = 0;
            while (taken < maxTake) {
                String next = args[start + taken];
                if (next != null && next.startsWith("-") && findParameter(list, next.substring(1)) != null) break;
                taken++;
            }

            if (taken < param.minArgs()) { remaining.add(token); i++; continue; }

            String[] paramArgs = Arrays.copyOfRange(args, start, start + taken);
            for (String arg : paramArgs) {
                if (!param.isWhitelisted(arg)) {
                    if (param.whitelistMessage() != null) message(sender, param.whitelistMessage());
                    return new ParameterParseResult(new String[0], true);
                }
            }

            alreadyUsed.add(param);
            if (sender instanceof Player p) {
                activeParameters.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(param, paramArgs);
            }
            param.execute(sender, fullArgs, paramArgs);
            if (param.stopSubCommands()) stop = true;
            i = start + taken;
        }

        if (stop) return new ParameterParseResult(new String[0], true);
        return new ParameterParseResult(remaining.toArray(new String[0]), false);
    }

    default @Nullable ParameterTabContext findActiveParameterTabContext(
            @NotNull CommandSender sender, @NotNull String[] args) {
        List<Parameter> list = parameters.getOrDefault(this, List.of());
        if (list.isEmpty() || args.length == 0) return null;

        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token == null || token.isEmpty() || token.charAt(0) != '-') continue;
            Parameter param = findParameter(list, token.substring(1));
            if (param == null) continue;
            if (param.permission() != null && !sender.hasPermission(param.permission())) continue;

            int start = i + 1;
            int endExclusive = start;
            while (endExclusive < args.length) {
                String next = args[endExclusive];
                if (next != null && next.startsWith("-") && findParameter(list, next.substring(1)) != null) break;
                endExclusive++;
            }

            boolean cursorWithinThisParam = (args.length >= start && args.length <= endExclusive);
            if (!cursorWithinThisParam) continue;

            String[] soFarRaw = Arrays.copyOfRange(args, start, endExclusive);
            int effectiveCount = soFarRaw.length;
            boolean stillTyping = effectiveCount > 0 && soFarRaw[effectiveCount - 1] != null && !soFarRaw[effectiveCount - 1].isEmpty();
            if (effectiveCount > 0 && soFarRaw[effectiveCount - 1] != null && soFarRaw[effectiveCount - 1].isEmpty()) effectiveCount--;

            if (stillTyping && effectiveCount > param.maxArgs()) return null;
            if (!stillTyping && effectiveCount >= param.maxArgs()) return null;
            return new ParameterTabContext(param, soFarRaw);
        }
        return null;
    }

    default @Nullable Parameter findParameter(@NotNull List<Parameter> list, @NotNull String nameOrAlias) {
        for (Parameter p : list) {
            if (p.name().equalsIgnoreCase(nameOrAlias)) return p;
            for (String a : p.aliases()) if (a.equalsIgnoreCase(nameOrAlias)) return p;
        }
        return null;
    }

    default @NotNull List<String> dispatchTabComplete(
            @NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if (playerOnly() && !isPlayer(sender)) return List.of();
        if (permission() != null && !sender.hasPermission(permission())) return List.of();

        if (args.length > 1 && subCommands.containsKey(this)) {
            final String first = args[0];
            for (CommandBasis sc : subCommands.get(this)) {
                boolean matches = sc.name().equalsIgnoreCase(first)
                        || sc.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(first));
                if (!matches) continue;
                if (sc.permission() != null && !sender.hasPermission(sc.permission())) return List.of();
                return sc.dispatchTabComplete(sender, fullArgs, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (args.length > 0 && args[args.length - 1].startsWith("-")) {
            return filterCompletions(args, suggestParameters(sender));
        }

        ParameterTabContext ctx = findActiveParameterTabContext(sender, args);
        if (ctx != null) {
            return filterCompletions(args, ctx.getParameter().tabComplete(sender, fullArgs, ctx.getParameterArgsSoFar()));
        }

        return filterCompletions(args, new ArrayList<>(tabComplete(sender, fullArgs, args)));
    }

    default @NotNull List<String> suggestParameters(@NotNull CommandSender sender) {
        List<Parameter> list = parameters.getOrDefault(this, List.of());
        if (list.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (Parameter p : list) {
            if (p.permission() != null && !sender.hasPermission(p.permission())) continue;
            out.add("-" + p.name());
        }
        return out;
    }

    default @NotNull List<String> tabComplete(
            @NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        return List.of();
    }

    default boolean isPlayer(@NotNull CommandSender sender) { return sender instanceof Player; }

    default void message(CommandSender sender, @Nullable String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(TextFormatter.transform(sender instanceof Player ? (Player) sender : null, message));
    }

    default void message(CommandSender sender, @Nullable Component message) {
        if (message == null) return;
        sender.sendMessage(TextFormatter.transform(sender instanceof Player ? (Player) sender : null, message));
    }

    default void addSubCommand(SubCommand subCommand) {
        subCommands.computeIfAbsent(this, k -> new ArrayList<>()).add(subCommand);
    }

    default boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command,
                              @NotNull String label, @NotNull String @NotNull [] args) {
        clearParameters(sender);
        dispatch(sender, args, args);
        return true;
    }

    default List<String> onTabComplete(@NotNull CommandSender sender,
                                       @NotNull org.bukkit.command.Command command,
                                       @NotNull String label, @NotNull String @NotNull [] args) {
        return dispatchTabComplete(sender, args, args);
    }

    default @NotNull List<String> filterCompletions(@NotNull String[] args,
                                                    @NotNull Collection<String> candidates) {
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