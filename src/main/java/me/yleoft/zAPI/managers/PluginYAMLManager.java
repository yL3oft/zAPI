package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

/**
 * PluginYAMLManager class to manage commands and permissions for a Bukkit plugin.
 * It allows registering and unregistering commands and permissions dynamically.
 */
public class PluginYAMLManager {

    private PluginDescriptionFile file;
    private List<Command> cmds = new ArrayList<>();
    private List<String> perms = new ArrayList<>();
    private zAPI main;

    /**
     * Constructor to initialize the PluginYAMLManager with the zAPI instance.
     * @param zAPI The zAPI instance.
     */
    public PluginYAMLManager(@NotNull zAPI zAPI) {
        this.main = zAPI;
        this.file = main.getPlugin().getDescription();
    }

    /**
     * CommandExecutor that does nothing and returns false.
     * This is used as a placeholder for commands that are not registered.
     */
    public static final TabExecutor emptyExec = new TabExecutor() {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            return false;
        }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return null;
        }
    };

    /**
     * Syncs the commands registered in the server.
     * This method is used to ensure that all commands are properly registered.
     */
    public void syncCommands() {
        try {
            Method syncCommandsMethod = main.getPlugin().getServer().getClass().getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);
            syncCommandsMethod.invoke(main.getPlugin().getServer());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not invoke syncCommands method", e);
        }
    }

    /**
     * Unregisters all commands registered by this plugin.
     * This method is used to clean up commands when the plugin is disabled or reloaded.
     */
    public void unregisterCommands() {
        try {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            HashMap<String, Command> commandsToCheck = new HashMap<String, Command>();

            for (Command c : cmds) {
                commandsToCheck.put(c.getLabel().toLowerCase(), c);
                commandsToCheck.put(c.getName().toLowerCase(), c);
                c.getAliases().forEach(a -> commandsToCheck.put(a.toLowerCase(), c));
            }

            for (Entry<String, Command> check : commandsToCheck.entrySet()) {
                Command mappedCommand = knownCommands.get(check.getKey());
                if (check.getValue().equals(mappedCommand)) {
                    mappedCommand.unregister(commandMap);
                    knownCommands.remove(check.getKey());
                } else if (check.getValue() instanceof PluginCommand) {
                    PluginCommand checkPCmd = (PluginCommand) check.getValue();
                    if (mappedCommand instanceof PluginCommand) {
                        PluginCommand mappedPCmd = (PluginCommand) mappedCommand;
                        CommandExecutor mappedExec = mappedPCmd.getExecutor();

                        if (mappedExec != null && mappedExec.equals(checkPCmd.getExecutor())) {
                            mappedPCmd.setExecutor(null);
                            mappedPCmd.setTabCompleter(null);
                        }
                    }
                    checkPCmd.setExecutor(emptyExec);
                    checkPCmd.setTabCompleter(emptyExec);
                }

            }
            knownCommandsField.setAccessible(false);
        } catch (Exception exception) {}
    }

    /**
     * Registers a command with the specified name, executor, description, and aliases.
     * @param command The name of the command.
     * @param ce The CommandExecutor for the command.
     * @param description The description of the command.
     * @param aliases The aliases for the command.
     */
    public void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @NotNull String description, @NotNull String... aliases){
        if(!file.getCommands().containsKey(command)) {
            try {
                Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                c.setAccessible(true);
                f.setAccessible(true);

                PluginCommand cmd = c.newInstance(command, main.getPlugin());
                cmd.setDescription(description);
                cmd.setExecutor(ce);

                List<String> aliasesList = new ArrayList<>();
                try {
                    aliasesList = new ArrayList<>(Arrays.asList(aliases));
                }catch (Exception e) {
                }
                cmd.setAliases(aliasesList);

                CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
                commandMap.register(main.getPluginName(), cmd);
                cmds.add(cmd);

                main.getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        main.getColoredPluginName()+"&aLoaded command &e/"+command
                ));
            }catch (Exception e) {
                e.printStackTrace();
                main.getPlugin().getLogger().severe(main.getColoredPluginName()+"§4Couldn't load command §e"+command);
            }
        }else {
            main.getPlugin().getLogger().severe(main.getColoredPluginName()+"§4Couldn't load command §e"+command);
        }
    }

    /**
     * Registers a command with the specified name, executor, tab completer, description, and aliases.
     * @param command The name of the command.
     * @param ce The CommandExecutor for the command.
     * @param completer The TabCompleter for the command.
     * @param description The description of the command.
     * @param aliases The aliases for the command.
     */
    public void registerCommand(@NotNull String command, @NotNull CommandExecutor ce, @NotNull TabCompleter completer, @NotNull String description, @NotNull String... aliases){
        if(!file.getCommands().containsKey(command)) {
            try {
                Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                c.setAccessible(true);
                f.setAccessible(true);

                PluginCommand cmd = c.newInstance(command, main.getPlugin());
                cmd.setDescription(description);
                cmd.setExecutor(ce);
                cmd.setTabCompleter(completer);

                List<String> aliasesList = new ArrayList<>();
                try {
                    aliasesList = new ArrayList<>(Arrays.asList(aliases));
                }catch (Exception e) {
                }
                cmd.setAliases(aliasesList);

                CommandMap commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
                commandMap.register(main.getPluginName(), cmd);
                cmds.add(cmd);

                main.getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        main.getColoredPluginName()+"&aLoaded command &e/"+command
                ));
            }catch (Exception e) {
                e.printStackTrace();
                main.getPlugin().getLogger().severe(main.getColoredPluginName()+"§4Couldn't load command §e"+command);
            }
        }else {
            main.getPlugin().getLogger().severe(main.getColoredPluginName()+"§4Couldn't load command §e"+command);
        }
    }

    /**
     * Unregisters all permissions registered by this plugin.
     */
    public void unregisterPermissions() {
        for(String perm : perms) {
            unregisterPermission(perm);
        }
        Bukkit.getPluginManager().getPermissions().forEach(permission -> {
            if(permission.getName().startsWith(main.getPluginName().toLowerCase())) {
                unregisterPermission(permission);
            }
        });
    }

    public void unregisterPermission(@NotNull String permission) {
        Bukkit.getPluginManager().removePermission(permission);
    }
    public void unregisterPermission(@NotNull Permission permission) {
        Bukkit.getPluginManager().removePermission(permission);
    }

    public void registerPermission(@NotNull String permission) {
        Bukkit.getPluginManager().addPermission(new Permission(permission));
        perms.add(permission);
    }
    public void registerPermission(@NotNull String permission, @NotNull String description) {
        Bukkit.getPluginManager().addPermission(new Permission(permission, description));
        perms.add(permission);
    }
    public void registerPermission(@NotNull String permission, @NotNull PermissionDefault def) {
        Bukkit.getPluginManager().addPermission(new Permission(permission, def));
        perms.add(permission);
    }
    public void registerPermission(@NotNull String permission, @NotNull String description, @NotNull PermissionDefault def) {
        Bukkit.getPluginManager().addPermission(new Permission(permission, description, def));
        perms.add(permission);
    }
    public void registerPermission(@NotNull String permission, @NotNull String description, @NotNull PermissionDefault def, @NotNull Map<String, Boolean> children) {
        Bukkit.getPluginManager().addPermission(new Permission(permission, description, def, children));
        perms.add(permission);
    }

    /**
     * Registers a TabCompleter for the specified command.
     * @param command The name of the command.
     * @param tc The TabCompleter for the command.
     */
    public void registerTabCompleter(@NotNull String command, @NotNull TabCompleter tc){
        try {
            Objects.requireNonNull(main.getPlugin().getCommand(command)).setTabCompleter(tc);
        }catch (Exception e) {
        }
    }

    /**
     * Registers a Listener for the plugin.
     * @param l The Listener to register.
     */
    public void registerEvent(@NotNull Listener l) {
        main.getPlugin().getServer().getPluginManager().registerEvents(l, main.getPlugin());
    }

}
