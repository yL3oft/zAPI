package me.yleoft.zAPI;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.yleoft.zAPI.handlers.PlaceholderAPIHandler;
import me.yleoft.zAPI.listeners.*;
import me.yleoft.zAPI.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class of zAPI
 * <p>
 * This class is used to initialize zAPI and provide access to its features.
 */
public class zAPI {

    private static String VERSION;
    private static int bStatsId;

    static {
        YamlConfiguration settings = new YamlConfiguration();
        String defaultVersion = "1.0.0";
        int defaultBStatsId = 26888;
        try (InputStream in = zAPI.class.getClassLoader().getResourceAsStream("settings.yml")) {
            if (in != null) {
                settings.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                VERSION = settings.getString("version", defaultVersion);
                bStatsId = settings.getInt("bstats-id", defaultBStatsId);
            } else {
                VERSION = defaultVersion;
                bStatsId = defaultBStatsId;
            }
        } catch (IOException | InvalidConfigurationException e) {
            VERSION = defaultVersion;
            bStatsId = defaultBStatsId;
        }
    }

    public static final String customCommandNBT = "zAPI:customCommand";
    public static PlaceholderAPIHandler placeholderAPIHandler;
    public static boolean useNBTAPI = false;

    private static JavaPlugin plugin;
    private static String pluginName;
    private static String coloredPluginName;
    private static Object papi;
    private static boolean usingFolia;

    /**
     * Initialize zAPI
     * @param plugin The plugin that is using zAPI
     * @param pluginName The custom name of the plugin
     * @param coloredPluginName The custom colored name of the plugin
     * @param useNBTAPI If the plugin should use NBTAPI
     */
    public static void init(@NotNull JavaPlugin plugin, @NotNull String pluginName, @NotNull String coloredPluginName, boolean useNBTAPI) {
        zAPI.plugin = plugin;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            zAPI.usingFolia = true;
        } catch (ClassNotFoundException e) {
            zAPI.usingFolia = false;
        }
        zAPI.pluginName = pluginName;
        zAPI.coloredPluginName = coloredPluginName;
        zAPI.useNBTAPI = useNBTAPI;
        placeholderAPIHandler = new PlaceholderAPIHandler();
        PluginYAMLManager.registerEvent(new PlayerListeners());
        if(useNBTAPI) {
            PluginYAMLManager.registerEvent(new DupeFixerListeners());
            PluginYAMLManager.registerEvent(new ItemListeners());
        }
        plugin.getLogger().info("[zAPI] Initialized by " + plugin.getName());
        try {
            new zAPIMetrics(plugin, bStatsId);
            plugin.getLogger().info("[zAPI] Using the plugin '"+pluginName+"' to create a bStats instance!");
        }catch (Exception ignored) {
            plugin.getLogger().warning("[zAPI] Failed to create a bStats instance.");
        }
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
    public static Metrics startMetrics(int pluginId) {
        return new Metrics(plugin, pluginId);
    }

    private static class zAPIMetrics extends Metrics {

        /**
         * Creates a new Metrics instance.
         *
         * @param plugin    Your plugin instance.
         * @param serviceId The id of the service. It can be found at <a
         *                  href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
         */
        public zAPIMetrics(JavaPlugin plugin, int serviceId) {
            super(plugin, serviceId);
            addCustomChart(new SimplePie("zapi_version", () -> VERSION));
            addCustomChart(new DrilldownPie("parent_plugin", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                Map<String, Integer> entry = new HashMap<>();
                entry.put(getPlugin().getDescription().getVersion(), 1);
                map.put(pluginName, entry);
                return map;
            }));
            addCustomChart(new DrilldownPie("player_count", () -> {
                int players = Bukkit.getOnlinePlayers().size();
                return buildDistribution(players);
            }));
        }

        //<editor-fold desc="Player Count Distribution"
        private Map<String, Map<String, Integer>> buildDistribution(int players) {
            Map<String, Map<String, Integer>> outer = new HashMap<>();
            String outerBucket = outerBucket(players);
            String innerBucket = innerBucket(players);
            Map<String, Integer> inner = new HashMap<>();
            inner.put(innerBucket, 1);
            outer.put(outerBucket, inner);
            return outer;
        }

        private String outerBucket(int n) {
            if (n <= 0) return "0";
            if (n <= 10) return "1-10";
            if (n <= 25) return "11-25";
            if (n <= 50) return "26-50";
            if (n <= 100) return "51-100";
            if (n <= 200) return "101-200";
            return "200+";
        }

        private String innerBucket(int n) {
            if (n <= 0) return "0";
            if (n <= 10) {
                return String.valueOf(n);
            }
            if (n <= 25) {
                return rangeOfFive(n, 11, 25);
            }
            if (n <= 50) {
                return rangeOfFive(n, 26, 50);
            }
            if (n <= 100) {
                return rangeOfFive(n, 51, 100);
            }
            if (n <= 200) {
                return rangeOfFive(n, 101, 200);
            }
            return "200+";
        }

        private String rangeOfFive(int n, int start, int end) {
            int bucketStart = ((n - start) / 5) * 5 + start;
            int bucketEnd = Math.min(bucketStart + 4, end);
            return bucketStart + "-" + bucketEnd;
        }
        //</editor-fold>

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
     * Returns if the server is running on Folia
     * @return true if the server is running on Folia, false otherwise
     */
    public static boolean isFolia() {
        return usingFolia;
    }

    /**
     * Setups the VaultAPI economy
     * @throws RuntimeException if Vault is not present on the server
     */
    public static Economy setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            throw new RuntimeException("Vault is not present on the server");
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new RuntimeException("Unable to load Economy class on VaultAPI");
        }
        return rsp.getProvider();
    }

}