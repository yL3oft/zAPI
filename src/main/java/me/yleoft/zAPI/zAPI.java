package me.yleoft.zAPI;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.yleoft.zAPI.handlers.PlaceholderAPIHandler;
import me.yleoft.zAPI.listeners.*;
import me.yleoft.zAPI.managers.*;
import me.yleoft.zAPI.utils.StringUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class zAPI {

    private static zAPI zAPI;

    public static String customCommandNBT = "zAPI:customCommand";
    public static StringUtils stringUtils;
    public PlaceholderAPIHandler placeholderAPIHandler;

    private final JavaPlugin plugin;
    protected final PluginYAMLManager pym;
    protected final FileManager fm;
    protected String pluginName;
    protected String coloredPluginName;
    protected Object papi;
    protected Object economy;

    /**
     * Constructor for zAPI
     *
     * @param plugin The plugin that is using zAPI
     * @param pluginName The custom name of the plugin
     * @param coloredPluginName The custom colored name of the plugin
     */
    public zAPI(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName, boolean useNBTAPI) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.coloredPluginName = coloredPluginName;
        zAPI = zAPI.this;
        pym = new PluginYAMLManager(zAPI);
        fm = new FileManager(zAPI);
        stringUtils = new StringUtils(this);
        placeholderAPIHandler = new PlaceholderAPIHandler(this);
        if(useNBTAPI) {
            pym.registerEvent(new DupeFixerListeners(this));
            pym.registerEvent(new ItemListeners(this));
        }
        plugin.getLogger().info("[zAPI] Initialized by " + plugin.getName());
    }
    public zAPI(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName) {
        this(plugin, pluginName, coloredPluginName, false);
    }

    /**
     * Returns the class of the plugin that has initialized zAPI
     *
     * @return The class of the plugin using zAPI
     */
    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the custom name of the plugin that initialized zAPI
     *
     * @return The name of the plugin
     */
    @NotNull
    public String getPluginName() {
        return pluginName;
    }
    public void setPluginName(@NotNull String pluginName) {
        this.pluginName = pluginName;
    }

    /**
     * Returns the custom colored name of the plugin that initialized zAPI
     *
     * @return The colored name of the plugin
     */
    @NotNull
    public String getColoredPluginName() {
        return coloredPluginName;
    }
    public void setColoredPluginName(@NotNull String coloredPluginName) {
        this.coloredPluginName = coloredPluginName;
    }

    /**
     * Returns the {@link PluginYAMLManager}
     *
     * @return {@link PluginYAMLManager}
     */
    @NotNull
    public PluginYAMLManager getPluginYAMLManager() {
        return pym;
    }

    /**
     * Returns the {@link FileManager}
     *
     * @return {@link FileManager}
     */
    @NotNull
    public FileManager getFileManager() {
        return fm;
    }

    /**
     * Returns the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> of the plugin for you
     *
     * @return The {@link me.clip.placeholderapi.expansion.PlaceholderExpansion} class of the plugin
     * @throws RuntimeException if the PlaceholderAPI is not enabled
     */
    public PlaceholderExpansion getPlaceholderExpansion() {
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
    public void startMetrics(int pluginId) {
        Metrics metrics = new Metrics(plugin, pluginId);
    }

    /**
     * Set the {@link PlaceholderAPIHandler} for the plugin
     * @param handler The {@link PlaceholderAPIHandler} class of the plugin
     */
    public void setPlaceholderAPIHandler(@NotNull PlaceholderAPIHandler handler) {
        placeholderAPIHandler = handler;
    }

    /**
     * Returns the {@link PlaceholderAPIHandler} for the plugin
     *
     * @return The {@link PlaceholderAPIHandler} class of the plugin
     */
    public PlaceholderAPIHandler getPlaceholderAPIHandler() {
        return placeholderAPIHandler;
    }

    /**
     * Register the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> for you
     * @param expansion The {@link me.clip.placeholderapi.expansion.PlaceholderExpansion} class of the plugin
     */
    public void registerPlaceholderExpansion(@NotNull String author, @NotNull String version, boolean canRegister, boolean persist) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
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
    public void unregisterPlaceholderExpansion() {
        if(papi != null) {
            ((PlaceholderExpansion)papi).unregister();
        }
    }

    /**
     * Setups the VaultAPI economy
     * @throws RuntimeException if Vault is not present on the server
     */
    private void setupEconomy() {
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