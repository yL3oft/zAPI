package me.yleoft.zAPI.utility;

import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized PluginYAML for managing commands and permissions dynamically.
 * Java 17+ optimized with records, sealed classes, pattern matching, and text blocks.
 */
public abstract sealed class PluginYAML permits PluginYAML.ReflectionCache {

    private static final PluginDescriptionFile DESCRIPTION_FILE = zAPI.getPlugin().getDescription();
    private static final Map<Command, Double> REGISTERED_COMMANDS = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PERMISSIONS = ConcurrentHashMap.newKeySet();
    public static final Map<Player, Long> cacheCooldown = new HashMap<>();

    /**
     * Sealed helper class to encapsulate reflection caching logic.
     */
    static final class ReflectionCache extends PluginYAML {
        private static volatile CommandMap commandMapCache;
        private static volatile Field commandMapField;
        private static volatile Field knownCommandsField;
        private static volatile Constructor<PluginCommand> pluginCommandConstructor;
        private static volatile Method syncCommandsMethod;

        private ReflectionCache() {
            // Prevent instantiation
        }
    }

    /**
     * Empty TabExecutor used as a placeholder for unregistered commands.
     */
    public static final TabExecutor emptyExec = new TabExecutor() {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            return false;
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
            return List.of();
        }
    };

    /**
     * Retrieves the CommandMap using cached reflection.
     */
    private static CommandMap getCommandMap() throws ReflectiveOperationException {
        if (ReflectionCache.commandMapCache != null) {
            return ReflectionCache.commandMapCache;
        }

        synchronized (PluginYAML.class) {
            if (ReflectionCache.commandMapCache != null) {
                return ReflectionCache.commandMapCache;
            }

            if (ReflectionCache.commandMapField == null) {
                ReflectionCache.commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                ReflectionCache.commandMapField.setAccessible(true);
            }

            ReflectionCache.commandMapCache = (CommandMap) ReflectionCache.commandMapField.get(Bukkit.getPluginManager());
            return ReflectionCache.commandMapCache;
        }
    }

    /**
     * Retrieves the knownCommands map using cached reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands(CommandMap commandMap) throws ReflectiveOperationException {
        if (ReflectionCache.knownCommandsField == null) {
            synchronized (PluginYAML.class) {
                if (ReflectionCache.knownCommandsField == null) {
                    ReflectionCache.knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                    ReflectionCache.knownCommandsField.setAccessible(true);
                }
            }
        }
        return (Map<String, Command>) ReflectionCache.knownCommandsField.get(commandMap);
    }

    /**
     * Syncs commands with the server (for 1.13+ command completion).
     */
    public static void syncCommands() {
        try {
            if (ReflectionCache.syncCommandsMethod == null) {
                synchronized (PluginYAML.class) {
                    if (ReflectionCache.syncCommandsMethod == null) {
                        ReflectionCache.syncCommandsMethod = zAPI.getPlugin().getServer().getClass().getDeclaredMethod("syncCommands");
                        ReflectionCache.syncCommandsMethod.setAccessible(true);
                    }
                }
            }
            ReflectionCache.syncCommandsMethod.invoke(zAPI.getPlugin().getServer());
        } catch (NoSuchMethodException ignored) {
            // Server version doesn't support syncCommands
        } catch (ReflectiveOperationException e) {
            zAPI.getPluginLogger().warn("Failed to sync commands", e);
        }
    }

    /**
     * Generates all possible command keys (including plugin prefix and aliases).
     */
    private static Set<String> generateCommandKeys(Command command, String pluginPrefix) {
        Set<String> keys = new HashSet<>();
        String label = command.getLabel().toLowerCase(Locale.ROOT);
        String name = command.getName().toLowerCase(Locale.ROOT);

        keys.add(label);
        keys.add(name);
        command.getAliases().forEach(alias -> keys.add(alias.toLowerCase(Locale.ROOT)));

        if (!pluginPrefix.isEmpty()) {
            keys.add(pluginPrefix + ":" + label);
            keys.add(pluginPrefix + ":" + name);
            command.getAliases().forEach(alias -> keys.add(pluginPrefix + ":" + alias.toLowerCase(Locale.ROOT)));
        }

        return keys;
    }

    /**
     * Checks if a command belongs to our plugin using pattern matching.
     */
    private static boolean isOwnedByPlugin(Command command) {
        if (command instanceof PluginCommand pluginCommand) {
            try {
                Plugin owner = pluginCommand.getPlugin();
                return owner != null && owner.equals(zAPI.getPlugin());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Unregisters all commands registered by this plugin.
     */
    public static void unregisterCommands() {
        try {
            CommandMap commandMap = getCommandMap();
            Map<String, Command> knownCommands = getKnownCommands(commandMap);
            String pluginPrefix = Optional.of(Version.getName())
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .orElse("");

            Set<String> keysToRemove = new HashSet<>();

            for (Command command : REGISTERED_COMMANDS.keySet()) {
                Set<String> commandKeys = generateCommandKeys(command, pluginPrefix);

                for (String key : commandKeys) {
                    Command registered = knownCommands.get(key);
                    if (registered != null && (registered.equals(command) || isOwnedByPlugin(registered))) {
                        keysToRemove.add(key);
                        try {
                            registered.unregister(commandMap);
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (command instanceof PluginCommand pluginCommand) {
                    pluginCommand.setExecutor(emptyExec);
                    pluginCommand.setTabCompleter(emptyExec);
                }

                keysToRemove.forEach(knownCommands::remove);
                REGISTERED_COMMANDS.clear();
            }
        } catch (Exception exception) {
            zAPI.getPluginLogger().error("Failed to unregister commands", exception);
        }
    }

    /**
     * Unregisters a specific command by name.
     */
    public static void unregisterCommand(@NotNull String commandName) {
        try {
            CommandMap commandMap = getCommandMap();
            Map<String, Command> knownCommands = getKnownCommands(commandMap);
            String cmdLower = commandName.toLowerCase(Locale.ROOT);
            String pluginPrefix = Optional.of(Version.getName())
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .orElse("");

            Set<Command> matchingCommands = new HashSet<>();

            // Find all tracked commands that match
            for (Command command : REGISTERED_COMMANDS.keySet()) {
                if (command.getLabel().equalsIgnoreCase(commandName) ||
                        command.getName().equalsIgnoreCase(commandName) ||
                        command.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(commandName))) {
                    matchingCommands.add(command);
                }
            }

            Set<String> keysToCheck = new HashSet<>();
            keysToCheck.add(cmdLower);
            if (!pluginPrefix.isEmpty()) {
                keysToCheck.add(pluginPrefix + ":" + cmdLower);
            }

            // Remove from knownCommands
            for (String key : keysToCheck) {
                Command registered = knownCommands.get(key);
                if (registered != null && (matchingCommands.contains(registered) || isOwnedByPlugin(registered))) {
                    try {
                        registered.unregister(commandMap);
                    } catch (Exception ignored) {
                    }
                    knownCommands.remove(key);
                }
            }

            // Clean up executors and remove from tracking using pattern matching
            matchingCommands.forEach(command -> {
                if (command instanceof PluginCommand pluginCommand) {
                    pluginCommand.setExecutor(emptyExec);
                    pluginCommand.setTabCompleter(emptyExec);
                }
                REGISTERED_COMMANDS.remove(command);
            });

        } catch (Exception e) {
            zAPI.getPluginLogger().warn("Error unregistering command: " + commandName, e);
        }
    }

    /**
     * Registers a command with full configuration.
     */
    public static void registerCommand(me.yleoft.zAPI.command.Command commandClass) {
        String command = commandClass.name();
        String description = commandClass.description();
        List<String> aliases = commandClass.aliases();

        if (DESCRIPTION_FILE.getCommands().containsKey(command)) {
            zAPI.getPluginLogger().warn("<red>Command <yellow>/%s<red> is already registered in plugin.yml!".formatted(command));
            return;
        }

        try {
            if (ReflectionCache.pluginCommandConstructor == null) {
                synchronized (PluginYAML.class) {
                    if (ReflectionCache.pluginCommandConstructor == null) {
                        ReflectionCache.pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                        ReflectionCache.pluginCommandConstructor.setAccessible(true);
                    }
                }
            }

            PluginCommand cmd = ReflectionCache.pluginCommandConstructor.newInstance(command, zAPI.getPlugin());
            cmd.setDescription(description);
            cmd.setExecutor(commandClass);
            cmd.setTabCompleter(commandClass);

            if (aliases != null && !aliases.isEmpty()) {
                cmd.setAliases(aliases);
            }

            CommandMap commandMap = getCommandMap();
            commandMap.register(Version.getName(), cmd);
            REGISTERED_COMMANDS.put(cmd, commandClass.cooldownTime());

            zAPI.getPluginLogger().info("<green>Loaded command <yellow>/%s".formatted(command));

        } catch (Exception exception) {
            zAPI.getPluginLogger().error("<red>Failed to register command /%s".formatted(command), exception);
        }
    }

    /**
     * Checks if a command is registered in plugin.yml.
     */
    public static boolean isCommandRegistered(@NotNull String command) {
        return DESCRIPTION_FILE.getCommands().containsKey(command);
    }

    /**
     * Unregisters all tracked permissions.
     */
    public static void unregisterPermissions() {
        REGISTERED_PERMISSIONS.forEach(PluginYAML::unregisterPermission);
    }

    /**
     * Unregisters a permission by name or Permission object.
     */
    public static void unregisterPermission(@NotNull String permission) {
        Bukkit.getPluginManager().removePermission(permission);
        REGISTERED_PERMISSIONS.remove(permission);
    }

    public static void unregisterPermission(@NotNull Permission permission) {
        Bukkit.getPluginManager().removePermission(permission);
        REGISTERED_PERMISSIONS.remove(permission.getName());
    }

    /**
     * Registers a permission with full configuration.
     */
    public static void registerPermission(@NotNull String permission, @Nullable String description,
                                          @Nullable PermissionDefault defaultValue, @Nullable Map<String, Boolean> children) {
        if (Bukkit.getPluginManager().getPermission(permission) == null) {
            Permission perm = new Permission(permission, description, defaultValue, children);
            Bukkit.getPluginManager().addPermission(perm);
        }
        REGISTERED_PERMISSIONS.add(permission);
    }

    // Overloaded convenience methods for permissions
    public static void registerPermission(@NotNull String permission) {
        registerPermission(permission, null, null, null);
    }

    public static void registerPermission(@NotNull String permission, @Nullable String description) {
        registerPermission(permission, description, null, null);
    }

    public static void registerPermission(@NotNull String permission, @Nullable PermissionDefault defaultValue) {
        registerPermission(permission, null, defaultValue, null);
    }

    public static void registerPermission(@NotNull String permission, @Nullable String description,
                                          @Nullable PermissionDefault defaultValue) {
        registerPermission(permission, description, defaultValue, null);
    }

    /**
     * Registers a TabCompleter for an existing command.
     */
    public static void registerTabCompleter(@NotNull String command, @NotNull TabCompleter tabCompleter) {
        Optional.ofNullable(zAPI.getPlugin().getCommand(command))
                .ifPresent(cmd -> cmd.setTabCompleter(tabCompleter));
    }

    /**
     * Registers an event listener.
     */
    public static void registerEvent(@NotNull Listener listener) {
        zAPI.getPlugin().getServer().getPluginManager().registerEvents(listener, zAPI.getPlugin());
    }

    /**
     * Returns an immutable view of registered commands and their cooldowns.
     */
    public static Map<Command, Double> getCmds() {
        return Collections.unmodifiableMap(REGISTERED_COMMANDS);
    }
}