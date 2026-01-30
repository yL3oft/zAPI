package me.yleoft.zAPI;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.yleoft.zAPI.handlers.PlaceholdersHandler;
import me.yleoft.zAPI.hooks.HookRegistry;
import me.yleoft.zAPI.listeners.DupeFixerListeners;
import me.yleoft.zAPI.listeners.ItemListeners;
import me.yleoft.zAPI.listeners.PlayerListeners;
import me.yleoft.zAPI.logging.FileLogger;
import me.yleoft.zAPI.logging.Logger;
import me.yleoft.zAPI.utility.PluginYAML;
import me.yleoft.zAPI.utility.Version;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        placeholdersHandler = new PlaceholdersHandler() {
            @Override
            public @NotNull String getIdentifier() {
                return "";
            }

            @Override
            public @Nullable String applyHookPlaceholders(@Nullable OfflinePlayer player, @NotNull String params) {
                return "";
            }
        };
    }

    private static JavaPlugin plugin;
    private static FoliaLib foliaLib;
    private static Logger logger;
    private static Logger pluginLogger;
    private static PlaceholdersHandler placeholdersHandler;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    public static boolean useNBTAPI = false;

    public static void setPlugin(@NotNull JavaPlugin plugin) {
        if(zAPI.plugin == null) zAPI.plugin = plugin;
    }

    public static void preload(@NotNull JavaPlugin plugin, boolean loadHooks) {
        setPlugin(plugin);
        if(zAPI.logger == null) zAPI.logger = new Logger("[zAPI]");
        if(zAPI.foliaLib == null) zAPI.foliaLib = new FoliaLib(plugin);
        if(loadHooks) HookRegistry.preload();
    }
    public static void preload(@NotNull JavaPlugin plugin) {
        preload(plugin, true);
    }

    /**
     * Initializes zAPI with the given JavaPlugin instance.
     *
     * @param plugin    The JavaPlugin instance to associate with zAPI.
     * @param useNBTAPI A boolean indicating whether to use NBTAPI features.
     */
    public static void init(@NotNull JavaPlugin plugin, boolean useNBTAPI) {
        if(zAPI.plugin != null && zAPI.plugin != plugin) {
            throw new IllegalStateException("zAPI has already been initialized with a different plugin!");
        }
        preload(plugin, false);
        zAPI.logger.setDebugMode(false);
        zAPI.useNBTAPI = useNBTAPI;
        PluginYAML.registerEvent(new PlayerListeners());
        if(useNBTAPI) {
            PluginYAML.registerEvent(new DupeFixerListeners());
            PluginYAML.registerEvent(new ItemListeners());
        }
        logger.info("Initialized zAPI v" + VERSION + " using " + plugin.getName() + " v" + Version.getVersion());
        logger.info("Version found: " + Bukkit.getServer().getName() + " " + Bukkit.getServer().getBukkitVersion() + " | APIv " + Version.CURRENT_VERSION);
        //<editor-fold desc="bStats">
        try {
            logger.info("Using the plugin '"+Version.getName()+" v"+Version.getVersion()+"' to create a bStats instance!");
            Metrics metrics = startMetrics(26888);
            metrics.addCustomChart(new DrilldownPie("parent_plugin", () -> {
                Map<String, Map<String, Integer>> data = new HashMap<>();
                String pluginVersion = Version.getVersion();
                Map<String, Integer> subCategory = new HashMap<>();
                subCategory.put(pluginVersion, 1);
                data.put(Version.getName(), subCategory);
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
            HookRegistry.unload();
            HookRegistry.clearHooks();
            getScheduler().cancelAllTasks();
            HandlerList.unregisterAll(plugin);
            PluginYAML.unregisterPermissions();
            PluginYAML.unregisterCommands();
        }
        FileLogger.compressLogs();
        plugin = null;
        logger = null;
        pluginLogger = null;
        placeholdersHandler = null;
        foliaLib = null;
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
     * Retrieves the FoliaLib instance used for scheduling tasks.
     *
     * @return The {@link FoliaLib} instance.
     */
    @NotNull
    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    /**
     * Retrieves the PlatformScheduler instance for scheduling tasks.
     *
     * @return The {@link PlatformScheduler} instance.
     */
    @NotNull
    public static PlatformScheduler getScheduler() {
        return foliaLib.getScheduler();
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
     * Retrieves the Logger instance used for plugin-specific logging.
     *
     * @return The {@link Logger} instance.
     */
    public static Logger getPluginLogger() {
        return pluginLogger != null ? pluginLogger : logger;
    }

    /**
     * Retrieves the PlaceholdersHandler instance used for handling placeholders.
     *
     * @return The {@link PlaceholdersHandler} instance.
     */
    public static PlaceholdersHandler getPlaceholdersHandler() {
        return placeholdersHandler;
    }

    /**
     * Sets the Logger instance used for plugin-specific logging.
     *
     * @param logger The {@link Logger} instance to set.
     */
    public static void setPluginLogger(Logger logger) {
        zAPI.pluginLogger = logger;
    }

    /**
     * Sets a custom PlaceholdersHandler for handling placeholders.
     *
     * @param handler The {@link PlaceholdersHandler} instance to set.
     */
    public static void setPlaceholdersHandler(@NotNull PlaceholdersHandler handler) {
        zAPI.getLogger().debug("Setting custom PlaceholderAPIHandler: " + handler.getClass().getName());
        HookRegistry.PAPI.registerPlaceholderExpansion(handler);
        zAPI.placeholdersHandler = handler;
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
