package me.yleoft.zAPI;

import me.yleoft.zAPI.listeners.DupeFixerListeners;
import me.yleoft.zAPI.listeners.ItemListeners;
import me.yleoft.zAPI.logging.FileLogger;
import me.yleoft.zAPI.logging.Logger;
import me.yleoft.zAPI.utility.PluginYAML;
import me.yleoft.zAPI.utility.Version;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The main class for the zAPI library.
 * This class provides initialization methods and configuration handling for zAPI.
 */
public abstract class zAPI {

    private static String VERSION;

    static {
        YamlConfiguration settings = new YamlConfiguration();
        String defaultVersion = "1.0.0";
        try (InputStream in = zAPI.class.getClassLoader().getResourceAsStream("settings.yml")) {
            if (in != null) {
                settings.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                VERSION = settings.getString("version", defaultVersion);
            } else {
                VERSION = defaultVersion;
            }
        } catch (IOException | InvalidConfigurationException e) {
            VERSION = defaultVersion;
        }
    }

    private static JavaPlugin plugin;
    private static Logger logger;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    public static boolean useNBTAPI = false;

    /**
     * Initializes zAPI with the given JavaPlugin instance.
     *
     * @param plugin    The JavaPlugin instance to associate with zAPI.
     * @param useNBTAPI A boolean indicating whether to use NBTAPI features.
     */
    public static void init(@NotNull JavaPlugin plugin, boolean useNBTAPI) {
        zAPI.plugin = plugin;
        zAPI.logger = new Logger("[zAPI]");
        zAPI.useNBTAPI = useNBTAPI;
        if(useNBTAPI) {
            PluginYAML.registerEvent(new DupeFixerListeners());
            PluginYAML.registerEvent(new ItemListeners());
        }
        logger.info("Initialized zAPI v" + VERSION + " using " + plugin.getName() + " v" + plugin.getPluginMeta().getVersion());
        //<editor-fold desc="bStats">
        try {
            logger.info("Using the plugin '"+plugin.getPluginMeta().getName()+" v"+plugin.getPluginMeta().getVersion()+"' to create a bStats instance!");
            Metrics metrics = startMetrics(26888);
            metrics.addCustomChart(new DrilldownPie("parent_plugin", () -> {
                Map<String, Map<String, Integer>> data = new HashMap<>();
                String pluginVersion = plugin.getPluginMeta().getVersion();
                Map<String, Integer> subCategory = new HashMap<>();
                subCategory.put(pluginVersion, 1);
                data.put(plugin.getPluginMeta().getName(), subCategory);
                return data;
            }));
            metrics.addCustomChart(new SimplePie("zapi_version", () -> VERSION));
            metrics.addCustomChart(new SimplePie("use_nbtapi", () -> useNBTAPI ? "Yes" : "No"));
        }catch (Exception exception) {
            logger.warn("Failed to create a bStats instance.", exception);
        }
        //</editor-fold>
    }
    public static void init(@NotNull JavaPlugin plugin) {
        init(plugin, false);
    }

    /**
     * Disable zAPI & cleanup resources
     */
    public static void disable() {
        if(plugin != null) {
            if (!Version.USING_FOLIA) {
                Bukkit.getScheduler().cancelTasks(plugin);
            }
            HandlerList.unregisterAll(plugin);
            // TODO: unregisterPlaceholderExpansion();
            PluginYAML.unregisterPermissions();
        }
        FileLogger.compressLogs();
    }

    /**
     * Retrieves the JavaPlugin instance associated with zAPI.
     *
     * @return The {@link JavaPlugin} instance.
     */
    @NotNull
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Retrieves the MiniMessage instance used for text formatting.
     *
     * @return The {@link MiniMessage} instance.
     */
    @NotNull
    public static MiniMessage getMiniMessage() {
        return miniMessage;
    }

    /**
     * Retrieves the Logger instance used for logging zAPI.
     *
     * @return The {@link Logger} instance.
     */
    @NotNull
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Starts a bStats Metrics instance for the given plugin ID.
     *
     * @param pluginId The bStats plugin ID.
     * @return The {@link Metrics} instance.
     */
    @NotNull
    public static Metrics startMetrics(int pluginId) {
        return new Metrics(plugin, pluginId);
    }

}
