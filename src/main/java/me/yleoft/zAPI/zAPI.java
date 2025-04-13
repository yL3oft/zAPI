package me.yleoft.zAPI;

import me.clip.placeholderapi.expansion.*;
import me.yleoft.zAPI.listeners.*;
import me.yleoft.zAPI.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class zAPI {

    private static zAPI zAPI;

    public static String customCommandNBT = "zAPI:customCommand";

    private final JavaPlugin plugin;
    protected final PluginYAMLManager pym;
    protected final FileManager fm;
    protected String pluginName;
    protected String coloredPluginName;
    protected PlaceholderExpansion papi;
    protected Economy economy;

    /**
     * Constructor for zAPI
     *
     * @param plugin The plugin that is using zAPI
     * @param pluginName The custom name of the plugin
     * @param coloredPluginName The custom colored name of the plugin
     */
    public zAPI(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.coloredPluginName = coloredPluginName;
        zAPI = zAPI.this;
        pym = new PluginYAMLManager(zAPI);
        fm = new FileManager(zAPI);
        plugin.getLogger().info("[zAPI] Initialized by " + plugin.getName());
    }
    public zAPI(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName, boolean useNBTAPI) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.coloredPluginName = coloredPluginName;
        zAPI = zAPI.this;
        pym = new PluginYAMLManager(zAPI);
        fm = new FileManager(zAPI);
        if(useNBTAPI) {
            pym.registerEvent(new DupeFixerListeners(this));
            pym.registerEvent(new ItemListeners(this));
        }
        plugin.getLogger().info("[zAPI] Initialized by " + plugin.getName());
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
        return papi;
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
     * Register the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> for you
     * @param expansion The {@link me.clip.placeholderapi.expansion.PlaceholderExpansion} class of the plugin
     */
    public void registerPlaceholderExpansion(@NotNull PlaceholderExpansion expansion){
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papi = expansion;
            papi.register();
        }
    }

    /**
     * Unregisters the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a>
     */
    public void unregisterPlaceholderExpansion() {
        if(papi != null) {
            papi.unregister();
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