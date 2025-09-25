package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.Map.Entry;

/**
 * PluginYAMLManager class to manage commands and permissions for a Bukkit plugin.
 * It allows registering and unregistering commands and permissions dynamically.
 */
public abstract class PluginYAMLManager {

    private static final PluginDescriptionFile file = zAPI.getPlugin().getDescription();
    private static final HashMap<Command, Double> cmds = new HashMap<>();
    private static final List<String> perms = new ArrayList<>();
    public static final HashMap<Player, Long> cacheCooldown = new HashMap<>();

    /**
     * CommandExecutor that does nothing and returns false.
     * This is used as a placeholder for commands that are not registered.
     */
    public static final TabExecutor emptyExec = new TabExecutor() {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            return false;
        }
        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
            return null;
        }
    };

    /**
     * Syncs the commands registered in the server.
     * This method is used to ensure that all commands are properly registered.
     */
    public static void syncCommands() {
        try {
            Method syncCommandsMethod = zAPI.getPlugin().getServer().getClass().getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);
            syncCommandsMethod.invoke(zAPI.getPlugin().getServer());
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not invoke syncCommands method", e);
        }
    }

    /**
     * Unregisters all commands registered by this plugin.
     * This method is used to clean up commands when the plugin is disabled or reloaded.
     */
    public static void unregisterCommands() {
        try {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            HashMap<String, Command> commandsToCheck = new HashMap<>();

            String pluginNameLower = zAPI.getPluginName() == null ? "" : zAPI.getPluginName().toLowerCase(Locale.ROOT);

            for (Command c : cmds.keySet()) {
                commandsToCheck.put(c.getLabel().toLowerCase(Locale.ROOT), c);
                commandsToCheck.put(c.getName().toLowerCase(Locale.ROOT), c);
                for (String a : c.getAliases()) {
                    commandsToCheck.put(a.toLowerCase(Locale.ROOT), c);
                }

                if (!pluginNameLower.isEmpty()) {
                    commandsToCheck.put(pluginNameLower + ":" + c.getLabel().toLowerCase(Locale.ROOT), c);
                    commandsToCheck.put(pluginNameLower + ":" + c.getName().toLowerCase(Locale.ROOT), c);
                    for (String a : c.getAliases()) {
                        commandsToCheck.put(pluginNameLower + ":" + a.toLowerCase(Locale.ROOT), c);
                    }
                }
            }

            for (Entry<String, Command> check : commandsToCheck.entrySet()) {
                String key = check.getKey();
                Command registered = knownCommands.get(key);

                if (registered == null) continue;

                boolean isSameCommand = check.getValue().equals(registered);

                boolean isOurPluginCommand = false;
                if (registered instanceof PluginCommand) {
                    try {
                        PluginCommand pCmd = (PluginCommand) registered;
                        Plugin owning = pCmd.getPlugin();
                        if (owning != null && owning.equals(zAPI.getPlugin())) {
                            isOurPluginCommand = true;
                        }
                    } catch (Throwable ignored) {}
                }

                if (isSameCommand || isOurPluginCommand) {
                    try {
                        registered.unregister(commandMap);
                    } catch (Exception ignored) {}
                    knownCommands.remove(key);
                    continue;
                }

                if (check.getValue() instanceof PluginCommand) {
                    try {
                        PluginCommand checkPCmd = (PluginCommand) check.getValue();
                        if (registered instanceof PluginCommand) {
                            PluginCommand mappedPCmd = (PluginCommand) registered;
                            CommandExecutor mappedExec = mappedPCmd.getExecutor();

                            if (mappedExec != null && mappedExec.equals(checkPCmd.getExecutor())) {
                                mappedPCmd.setExecutor(null);
                                mappedPCmd.setTabCompleter(null);
                            }
                        }
                        checkPCmd.setExecutor(emptyExec);
                        checkPCmd.setTabCompleter(emptyExec);
                    } catch (Exception ignored) {
                    }
                }
            }

            knownCommandsField.setAccessible(false);
            f.setAccessible(false);

            cmds.clear();
        } catch (Exception ignored) {}
    }

    /**
     * Unregisters a command with the specified name.
     * @param command The name of the command to unregister.
     */
    public static void unregisterCommand(@NotNull String command) {
        try {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            String cmdLower = command.toLowerCase(Locale.ROOT);
            String pluginNameLower = zAPI.getPluginName() == null ? "" : zAPI.getPluginName().toLowerCase(Locale.ROOT);

            List<String> keysToCheck = new ArrayList<>();
            keysToCheck.add(cmdLower);
            if (!pluginNameLower.isEmpty()) keysToCheck.add(pluginNameLower + ":" + cmdLower);

            Set<Command> trackedMatching = new HashSet<>();
            for (Command c : cmds.keySet()) {
                if (c.getLabel().equalsIgnoreCase(command) || c.getName().equalsIgnoreCase(command)) {
                    trackedMatching.add(c);
                    continue;
                }
                for (String a : c.getAliases()) {
                    if (a.equalsIgnoreCase(command)) {
                        trackedMatching.add(c);
                        break;
                    }
                }
            }

            for (String key : keysToCheck) {
                Command mapped = knownCommands.get(key);
                if (mapped == null) continue;

                boolean isSame = trackedMatching.contains(mapped);

                boolean isOurPluginCommand = false;
                if (mapped instanceof PluginCommand) {
                    try {
                        PluginCommand pCmd = (PluginCommand) mapped;
                        Plugin owning = pCmd.getPlugin();
                        if (owning != null && owning.equals(zAPI.getPlugin())) {
                            isOurPluginCommand = true;
                        }
                    } catch (Throwable ignored) {}
                }

                if (isSame || isOurPluginCommand) {
                    try {
                        mapped.unregister(commandMap);
                    } catch (Exception ignored) {}
                    knownCommands.remove(key);
                    continue;
                }

                for (Command t : trackedMatching) {
                    if (!(t instanceof PluginCommand)) continue;
                    try {
                        PluginCommand checkPCmd = (PluginCommand) t;
                        if (mapped instanceof PluginCommand) {
                            PluginCommand mappedPCmd = (PluginCommand) mapped;
                            CommandExecutor mappedExec = mappedPCmd.getExecutor();
                            if (mappedExec != null && mappedExec.equals(checkPCmd.getExecutor())) {
                                mappedPCmd.setExecutor(null);
                                mappedPCmd.setTabCompleter(null);
                            }
                        }
                        checkPCmd.setExecutor(emptyExec);
                        checkPCmd.setTabCompleter(emptyExec);
                    } catch (Exception ignored) {}
                }
            }

            for (Command t : trackedMatching) {
                cmds.remove(t);
            }

            knownCommandsField.setAccessible(false);
            f.setAccessible(false);
        } catch (Exception ignored) {}
    }

    /**
     * Registers a command with the specified name, executor, tab completer, description, and aliases.
     * @param command The name of the command.
     * @param ce The CommandExecutor for the command.
     * @param completer The TabCompleter for the command.
     * @param description The description of the command.
     * @param aliases The aliases for the command.
     */
    public static void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @Nullable Double cooldown, @Nullable TabCompleter completer, @NotNull String description, @NotNull String... aliases){
        if(cooldown == null) cooldown = 0d;
        if(!file.getCommands().containsKey(command)) {
            try {
                Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                c.setAccessible(true);
                f.setAccessible(true);

                PluginCommand cmd = c.newInstance(command, zAPI.getPlugin());
                cmd.setDescription(description);
                cmd.setExecutor(ce);
                if(completer != null) cmd.setTabCompleter(completer);

                List<String> aliasesList = new ArrayList<>();
                try {
                    aliasesList = new ArrayList<>(Arrays.asList(aliases));
                }catch (Exception ignored) {
                }
                cmd.setAliases(aliasesList);

                CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
                commandMap.register(zAPI.getPluginName(), cmd);
                cmds.put(cmd, cooldown);

                zAPI.getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        zAPI.getColoredPluginName()+"&aLoaded command &e/"+command
                ));
            }catch (Exception e) {
                e.printStackTrace();
                zAPI.getPlugin().getLogger().severe(zAPI.getColoredPluginName()+"§4Couldn't load command §e"+command);
            }
        }else {
            zAPI.getPlugin().getLogger().severe(zAPI.getColoredPluginName()+"§4Couldn't load command §e"+command+"§4, it is already registered!");
        }
    }

    public static void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @Nullable TabCompleter completer, @NotNull String description, @NotNull String... aliases){
        registerCommand(command, ce, null, completer, description, aliases);
    }
    public static void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @Nullable Double cooldown, @NotNull String description, @NotNull String... aliases){
        registerCommand(command, ce, cooldown, null, description, aliases);
    }
    public static void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @NotNull String description, @NotNull String... aliases){
        registerCommand(command, ce, null, null, description, aliases);
    }

    /**
     * Checks if a command is registered in the plugin's plugin.yml file.
     * @param command The name of the command to check.
     * @return true if the command is registered, false otherwise.
     */
    public static boolean isCommandRegistered(@NotNull String command) {
        return file.getCommands().containsKey(command);
    }

    /**
     * Unregisters all permissions registered by this plugin.
     */
    public static void unregisterPermissions() {
        for(String perm : perms) {
            unregisterPermission(perm);
        }
    }

    /**
     * Unregisters a permission with the specified name or Permission object.
     * @param permission The name of the permission or the Permission object to unregister.
     */
    public static void unregisterPermission(@NotNull String permission) {
        Bukkit.getPluginManager().removePermission(permission);
    }
    public static void unregisterPermission(@NotNull Permission permission) {
        Bukkit.getPluginManager().removePermission(permission);
    }

    /**
     * Registers a permission with the specified name, description, default value, and children.
     * @param permission The name of the permission.
     * @param description The description of the permission.
     * @param def The default value of the permission.
     * @param children The children permissions.
     */
    public static void registerPermission(@NotNull String permission, @Nullable String description, @Nullable PermissionDefault def, @Nullable Map<String, Boolean> children) {
        if (Bukkit.getPluginManager().getPermission(permission) == null) {
            Bukkit.getPluginManager().addPermission(new Permission(permission, description, def, children));
        }
        if(!perms.contains(permission)) perms.add(permission);
    }
    public static void registerPermission(@NotNull String permission) {
        registerPermission(permission, null, null, null);
    }
    public static void registerPermission(@NotNull String permission, @Nullable String description) {
        registerPermission(permission, description, null, null);
    }
    public static void registerPermission(@NotNull String permission, @Nullable PermissionDefault def) {
        registerPermission(permission, null, def, null);
    }
    public static void registerPermission(@NotNull String permission, @Nullable String description, @Nullable PermissionDefault def) {
        registerPermission(permission, description, def, null);
    }

    /**
     * Registers a TabCompleter for the specified command.
     * @param command The name of the command.
     * @param tc The TabCompleter for the command.
     */
    public static void registerTabCompleter(@NotNull String command, @NotNull TabCompleter tc){
        try {
            Objects.requireNonNull(zAPI.getPlugin().getCommand(command)).setTabCompleter(tc);
        }catch (Exception ignored) {
        }
    }

    /**
     * Registers a Listener for the plugin.
     * @param l The Listener to register.
     */
    public static void registerEvent(@NotNull Listener l) {
        zAPI.getPlugin().getServer().getPluginManager().registerEvents(l, zAPI.getPlugin());
    }

    public static HashMap<Command, Double> getCmds() {
        return cmds;
    }

}
