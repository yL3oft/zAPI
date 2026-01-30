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

/**
 * Basis for commands with subcommands and parameters.
 */
public interface CommandBasis extends CommandExecutor, TabCompleter {

    Map<CommandBasis, List<SubCommand>> subCommands = new HashMap<>();
    Map<CommandBasis, List<Parameter>> parameters = new HashMap<>();
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

    /**
     * The main execution logic of this command.
     */
    void execute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String @NotNull [] args);

    /**
     * Adds a parameter to this command.
     */
    default void addParameter(@NotNull Parameter parameter) {
        parameters.computeIfAbsent(this, k -> new ArrayList<>()).add(parameter);
    }

    /**
     * Returns the args provided to this parameter for the current command run, or null if not used.
     */
    default @Nullable String[] getParameter(@NotNull CommandSender sender, @NotNull Parameter parameter) {
        if (!(sender instanceof Player p)) return null;
        Map<Parameter, String[]> map = activeParameters.get(p.getUniqueId());
        return map == null ? null : map.get(parameter);
    }

    default void clearParameters(@NotNull CommandSender sender) {
        if (sender instanceof Player p) activeParameters.remove(p.getUniqueId());
    }

    default void dispatch(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if (playerOnly() && !isPlayer(sender)) {
            message(sender, playerOnlyMessage());
            return;
        }
        if (permission() != null && !sender.hasPermission(permission())) {
            message(sender, permissionMessage());
            return;
        }

        ParameterParseResult parsed = parseAndExecuteParameters(sender, fullArgs, args);
        if (parsed.stopFurtherDispatch) return;
        args = parsed.remainingArgs;

        if (prexecute(sender, fullArgs, args)) return;

        if (minArgs() > 0 && args.length < minArgs()) {
            message(sender, usage(sender, fullArgs, args));
            return;
        }
        if (args.length > maxArgs()) {
            message(sender, usage(sender, fullArgs, args));
            return;
        }

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

    final class ParameterParseResult {
        final String[] remainingArgs;
        final boolean stopFurtherDispatch;

        ParameterParseResult(String[] remainingArgs, boolean stopFurtherDispatch) {
            this.remainingArgs = remainingArgs;
            this.stopFurtherDispatch = stopFurtherDispatch;
        }
    }

    /**
     * Updated rules:
     * - Parses parameters ANYWHERE in args for this command level (not inherited by parents/subcommands)
     * - Parameters are not repeatable
     * - Only triggers if sender has the parameter permission (if set)
     * - Parameter args are taken greedily until:
     *    - maxArgs reached, OR
     *    - the next token is another known "-param"
     * - Parameter tokens + their args are REMOVED from the args passed down to execute/subcommand logic
     */
    default @NotNull ParameterParseResult parseAndExecuteParameters(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        List<Parameter> list = parameters.getOrDefault(this, List.of());
        if (list.isEmpty() || args.length == 0) return new ParameterParseResult(args, false);

        boolean stop = false;

        Set<Parameter> alreadyUsed = new HashSet<>();
        List<String> remaining = new ArrayList<>(args.length);

        int i = 0;
        while (i < args.length) {
            String token = args[i];

            // Not a parameter token => keep as positional arg
            if (token == null || token.isEmpty() || token.charAt(0) != '-') {
                remaining.add(token);
                i++;
                continue;
            }

            String rawName = token.substring(1);
            Parameter param = findParameter(list, rawName);

            // Unknown "-something" => treat as normal arg (positional)
            if (param == null) {
                remaining.add(token);
                i++;
                continue;
            }

            // Not repeatable: keep the later occurrence as positional text (or skip it).
            if (alreadyUsed.contains(param)) {
                // safest: treat it as normal arg so user can see/handle it if they want
                remaining.add(token);
                i++;
                continue;
            }

            // Permission not met => treat as positional text (so your command can error if desired)
            if (param.permission() != null && !sender.hasPermission(param.permission())) {
                remaining.add(token);
                i++;
                continue;
            }

            int start = i + 1;
            int maxTake = Math.min(param.maxArgs(), args.length - start);

            int taken = 0;
            while (taken < maxTake) {
                String next = args[start + taken];
                if (next != null && next.startsWith("-") && findParameter(list, next.substring(1)) != null) {
                    break;
                }
                taken++;
            }

            // Not enough args for this param => treat "-param" as positional and continue
            if (taken < param.minArgs()) {
                remaining.add(token);
                i++;
                continue;
            }

            String[] paramArgs = Arrays.copyOfRange(args, start, start + taken);
            alreadyUsed.add(param);

            if (sender instanceof Player p) {
                activeParameters.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(param, paramArgs);
            }

            param.execute(sender, fullArgs, paramArgs);

            if (param.stopSubCommands()) stop = true;

            // Skip "-param" + its args (removed from remaining)
            i = start + taken;
        }

        if (stop) return new ParameterParseResult(new String[0], true);
        return new ParameterParseResult(remaining.toArray(new String[0]), false);
    }

    final class ParameterTabContext {
        final Parameter parameter;
        final String[] parameterArgsSoFar;

        ParameterTabContext(Parameter parameter, String[] parameterArgsSoFar) {
            this.parameter = parameter;
            this.parameterArgsSoFar = parameterArgsSoFar;
        }
    }

    /**
     * If the user is currently typing args for SOME parameter in this args list (this command level),
     * return which parameter and the args typed for it so far. Otherwise null.
     *
     * Respects maxArgs(): once maxArgs are already provided, this returns null (no more parameter-arg tab).
     */
    // Replace ONLY the findActiveParameterTabContext(...) method with this version.

    default @Nullable ParameterTabContext findActiveParameterTabContext(@NotNull CommandSender sender, @NotNull String[] args) {
        List<Parameter> list = parameters.getOrDefault(this, List.of());
        if (list.isEmpty() || args.length == 0) return null;

        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token == null || token.isEmpty() || token.charAt(0) != '-') continue;

            Parameter param = findParameter(list, token.substring(1));
            if (param == null) continue;

            if (param.permission() != null && !sender.hasPermission(param.permission())) continue;

            int start = i + 1;

            // Find end of this parameter segment (next known parameter token or end)
            int endExclusive = start;
            while (endExclusive < args.length) {
                String next = args[endExclusive];
                if (next != null && next.startsWith("-") && findParameter(list, next.substring(1)) != null) {
                    break;
                }
                endExclusive++;
            }

            // Cursor must be within this param segment (this param is the last recognized before cursor)
            boolean cursorWithinThisParam = (args.length >= start && args.length <= endExclusive);
            if (!cursorWithinThisParam) continue;

            String[] soFarRaw = Arrays.copyOfRange(args, start, endExclusive);

            // IMPORTANT: Bukkit often includes a trailing "" when the user just typed a space and pressed tab.
            // That "" means "currently typing the next arg" and should not count as a completed argument.
            int effectiveCount = soFarRaw.length;
            boolean stillTyping = effectiveCount > 0 && soFarRaw[effectiveCount - 1] != null && !soFarRaw[effectiveCount - 1].isEmpty();
            if (effectiveCount > 0 && soFarRaw[effectiveCount - 1] != null && soFarRaw[effectiveCount - 1].isEmpty()) {
                effectiveCount--;
            }

            // If still typing current arg, it's not complete yet
            if (stillTyping && effectiveCount > param.maxArgs()) return null;
            if (!stillTyping && effectiveCount >= param.maxArgs()) return null;

            // Keep the raw array (including possible trailing ""), so the parameter tabComplete can
            // use it to know partial input if it wants.
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

    default @NotNull List<String> dispatchTabComplete(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String[] args) {
        if (playerOnly() && !isPlayer(sender)) return List.of();
        if (permission() != null && !sender.hasPermission(permission())) return List.of();

        // 1) Route to subcommand FIRST if applicable
        if (args.length > 1 && subCommands.containsKey(this)) {
            final String first = args[0];
            for (CommandBasis sc : subCommands.get(this)) {
                boolean matches = sc.name().equalsIgnoreCase(first)
                        || sc.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(first));
                if (!matches) continue;

                if (sc.permission() != null && !sender.hasPermission(sc.permission())) return List.of();

                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return sc.dispatchTabComplete(sender, fullArgs, subArgs);
            }
        }

        if (args.length > 0) {
            String last = args[args.length - 1];
            if (last.startsWith("-")) {
                return filterCompletions(args, suggestParameters(sender));
            }
        }

        // 2) If currently typing parameter arguments, delegate to that parameter
        ParameterTabContext ctx = findActiveParameterTabContext(sender, args);
        if (ctx != null) {
            List<String> suggestions = ctx.parameter.tabComplete(sender, fullArgs, ctx.parameterArgsSoFar);
            return filterCompletions(args, suggestions);
        }

        List<String> base = new ArrayList<>(tabComplete(sender, fullArgs, args));
        return filterCompletions(args, base);
    }

    /**
     * Suggest parameters only after user typed '-', and only if permission is satisfied.
     * Returns "-name" and "-alias" forms.
     */
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
        clearParameters(sender);
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