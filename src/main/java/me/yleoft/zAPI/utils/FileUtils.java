package me.yleoft.zAPI.utils;

import com.google.common.base.Charsets;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.logging.Level;

/**
 * FileUtils is a utility class for managing configuration files in a Bukkit plugin.
 * It provides methods to load, save, and reload configuration files, as well as to save embedded resources.
 */
public class FileUtils {

    private zAPI zAPI;

    private FileConfiguration newConfig = null;
    private File configFile = null;
    private String resource = null;

    /**
     * Creates a new FileUtils instance.
     *
     * @param zAPI      The zAPI instance.
     * @param f         The file to be used.
     * @param resource   The resource path.
     */
    public FileUtils(@NotNull zAPI zAPI, @NotNull File f, @NotNull String resource) {
        this.zAPI = zAPI;
        this.configFile = f;
        this.resource = resource;
    }

    /**
     * Gets the configuration file.
     * @return The configuration file.
     */
    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    /**
     * Gets the resource path.
     * @return The resource path.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Gets the file.
     * @return The file.
     */
    public File getFile() {
        return configFile;
    }

    /**
     * Reloads the configuration file.
     */
    public void reloadConfig() {
        newConfig = YamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream = zAPI.getPlugin().getResource(resource);
        if (defConfigStream == null) {
            return;
        }

        newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    /**
     * Saves the configuration file.
     */
    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    /**
     * Saves the default configuration file if it does not exist.
     */
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource(resource, false);
        }
    }

    /**
     * Saves an embedded resource to the plugin's data folder.
     *
     * @param resourcePath The path to the resource.
     * @param replace      Whether to replace the existing file if it exists.
     * @throws IllegalArgumentException if the resource path is null or empty.
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = zAPI.getPlugin().getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + zAPI.getPluginName());
        }

        File outFile = new File(zAPI.getPlugin().getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(zAPI.getPlugin().getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();

                zAPI.getPlugin().getServer().getConsoleSender().sendMessage(
                        zAPI.getColoredPluginName()+"§eFile §e'"+resourcePath+"' §fhave been created!"
                );
            } else {
                zAPI.getPlugin().getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

}
