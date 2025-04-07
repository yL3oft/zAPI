package me.yleoft.zAPI;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.yleoft.zAPI.managers.PluginYAMLManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class zAPI {

    private static zAPI zAPI;

    private final JavaPlugin plugin;
    protected final PluginYAMLManager pym;
    protected final String pluginName;
    protected final String coloredPluginName;
    protected PlaceholderExpansion papi;
    protected Economy economy;

    private zAPI(JavaPlugin plugin, String pluginName, String coloredPluginName) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.coloredPluginName = coloredPluginName;
        pym = new PluginYAMLManager();
        plugin.getLogger().info("[zAPI] Initialized with " + plugin.getName());
    }

    /**
     * Initializes the zAPI class
     * @param plugin The class of the plugin
     * @param pluginName Custom name of the plugin
     * @param coloredPluginName Custom colored name of the plugin
     */
    public static void init(JavaPlugin plugin, String pluginName, String coloredPluginName) {
        if (zAPI == null) {
            zAPI = new zAPI(plugin, pluginName, coloredPluginName);
        }
    }

    /**
     * Returns the zAPI class so other methods are accessible
     *
     * @return This class
     */
    public static zAPI getInstance() {
        if (zAPI == null) {
            throw new IllegalStateException("zAPI has not been initialized. This in an developer error!");
        }
        return zAPI;
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

    /**
     * Returns the custom colored name of the plugin that initialized zAPI
     *
     * @return The colored name of the plugin
     */
    @NotNull
    public String getColoredPluginName() {
        return coloredPluginName;
    }

    /**
     * Returns the <a href="https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion">PlaceholderAPI Expansion</a> of the plugin for you
     *
     * @return The {@link me.clip.placeholderapi.expansion.PlaceholderExpansion} class of the plugin
     */
    public PlaceholderExpansion getPlaceholderExpansion() {
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
     *
     * @throws RuntimeException
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