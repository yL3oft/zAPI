package me.yleoft.zAPI;

import de.tr7zw.changeme.nbtapi.NBT;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.yleoft.zAPI.handlers.PlaceholderAPIHandler;
import me.yleoft.zAPI.listeners.*;
import me.yleoft.zAPI.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class zAPI {

    public static String customCommandNBT = "zAPI:customCommand";
    public static PlaceholderAPIHandler placeholderAPIHandler;

    protected static JavaPlugin plugin;
    protected static String pluginName;
    protected static String coloredPluginName;
    protected static Object papi;
    protected static Object economy;

    /**
     * Initialize zAPI
     * @param plugin The plugin that is using zAPI
     * @param pluginName The custom name of the plugin
     * @param coloredPluginName The custom colored name of the plugin
     * @param useNBTAPI If the plugin should use NBTAPI
     */
    public static void init(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName, boolean useNBTAPI) {
        zAPI.plugin = plugin;
        zAPI.pluginName = pluginName;
        zAPI.coloredPluginName = coloredPluginName;
        placeholderAPIHandler = new PlaceholderAPIHandler();
        if(useNBTAPI) {
            PluginYAMLManager.registerEvent(new DupeFixerListeners());
            PluginYAMLManager.registerEvent(new ItemListeners());
        }
        plugin.getLogger().info("[zAPI] Initialized by " + plugin.getName());
    }
    public static void init(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName) {
        init(plugin, pluginName, coloredPluginName, false);
    }

    /**
     * Returns the class of the plugin that has initialized zAPI
     *
     * @return The class of the plugin using zAPI
     */
    @NotNull
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the custom name of the plugin that initialized zAPI
     *
     * @return The name of the plugin
     */
    @NotNull
    public static String getPluginName() {
        return pluginName;
    }
    public static void setPluginName(@NotNull String pluginName) {
        zAPI.pluginName = pluginName;
    }

    /**
     * Returns the custom colored name of the plugin that initialized zAPI
     *
     * @return The colored name of the plugin
     */
    @NotNull
    public static String getColoredPluginName() {
        return coloredPluginName;
    }
    public static void setColoredPluginName(@NotNull String coloredPluginName) {
        zAPI.coloredPluginName = coloredPluginName;
    }

    /**
     * Returns the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> of the plugin for you
     *
     * @return The {@link me.clip.placeholderapi.expansion.PlaceholderExpansion} class of the plugin
     * @throws RuntimeException if the PlaceholderAPI is not enabled
     */
    public static PlaceholderExpansion getPlaceholderExpansion() {
        if(papi == null) {
            throw new RuntimeException("PlaceholderAPI is not enabled");
        }
        return (PlaceholderExpansion) papi;
    }

    /**
     * Start the plugin metrics on <a href="https://bstats.org/">bStats</a>
     *
     * @param pluginId The ID of the plugin on <a href="https://bstats.org/">bStats</a>
     */
    public static void startMetrics(int pluginId) {
        Metrics metrics = new Metrics(plugin, pluginId);
    }

    /**
     * Set the {@link PlaceholderAPIHandler} for the plugin
     * @param handler The {@link PlaceholderAPIHandler} class of the plugin
     */
    public static void setPlaceholderAPIHandler(@NotNull PlaceholderAPIHandler handler) {
        placeholderAPIHandler = handler;
    }

    /**
     * Returns the {@link PlaceholderAPIHandler} for the plugin
     *
     * @return The {@link PlaceholderAPIHandler} class of the plugin
     */
    public static PlaceholderAPIHandler getPlaceholderAPIHandler() {
        return placeholderAPIHandler;
    }

    /**
     * Register the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> for you
     * @param author The author of the plugin
     * @param version The version of the plugin
     * @param canRegister If the expansion can be registered
     * @param persist If the expansion should persist
     */
    public static void registerPlaceholderExpansion(@NotNull String author, @NotNull String version, boolean canRegister, boolean persist) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = new PlaceholderExpansion() {
                @Override
                public @NotNull String getIdentifier() {
                    return plugin.getDescription().getName().toLowerCase();
                }

                @Override
                public @NotNull String getAuthor() {
                    return author;
                }

                @Override
                public @NotNull String getVersion() {
                    return version;
                }

                @Override
                public boolean canRegister() {
                    return canRegister;
                }

                @Override
                public boolean persist() {
                    return persist;
                }

                @Override
                public String onPlaceholderRequest(Player p, @NotNull String params) {
                    return placeholderAPIHandler.applyHookPlaceholders(p, params);
                }
            };
            ((PlaceholderExpansion)papi).register();
        }
    }

    /**
     * Unregisters the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a>
     */
    public static void unregisterPlaceholderExpansion() {
        if(papi != null) {
            ((PlaceholderExpansion)papi).unregister();
        }
    }

    /**
     * Setups the VaultAPI economy
     * @throws RuntimeException if Vault is not present on the server
     */
    private static void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            throw new RuntimeException("Vault is not present on the server");
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new RuntimeException("Unable to load Economy class on VaultAPI");
        }
        economy = rsp.getProvider();
    }

}