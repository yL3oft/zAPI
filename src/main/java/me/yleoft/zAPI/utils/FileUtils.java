package me.yleoft.zAPI.utils;

import com.google.common.base.Charsets;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * FileUtils is a utility class for managing configuration files in a Bukkit plugin.
 * It provides methods to load, save, and reload configuration files, as well as to save embedded resources.
 */
public class FileUtils {

    private FileConfiguration newConfig = null;
    private final File configFile;
    private final String resource;

    /**
     * Creates a new FileUtils instance.
     *
     * @param file       The file to be used.
     * @param resource   The resource path.
     */
    public FileUtils(@NotNull File file, @NotNull String resource) {
        this.configFile = file;
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

    public void reloadConfig(boolean copyDefaults) {
        try {
            newConfig = new YamlConfiguration();
            newConfig.load(configFile);

            final InputStream defConfigStream = zAPI.getPlugin().getResource(resource);
            if (defConfigStream != null) {
                newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
            }

            saveDefaultConfig();
            getConfig().options().copyDefaults(copyDefaults);
            saveConfig();
        } catch (IOException | InvalidConfigurationException ex) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Failed to load YAML file: " + configFile.getName(), ex);

            zAPI.getPlugin().getServer().getConsoleSender().sendMessage(zAPI.getColoredPluginName()
                    + "§cError loading §f" + configFile.getName() + "§c! Backing up and restoring default config...");

            backupBrokenConfig();
            saveDefaultConfig(); // Restore default
            reloadConfig(copyDefaults); // Try again with fresh config
        }
    }
    public void reloadConfig() {
        reloadConfig(true);
    }

    /**
     * Saves the configuration file.
     */
    public void saveConfig() {
        try {
            if (newConfig != null) {
                newConfig.save(configFile);
            }
        } catch (IOException ex) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Could not save config to " + configFile.getName(), ex);
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
     * Backs up the configuration file if it is broken.
     */
    private void backupBrokenConfig() {
        if (!configFile.exists()) return;

        String backupName = configFile.getName() + ".broken." + System.currentTimeMillis();
        File backupFile = new File(configFile.getParentFile(), backupName);

        try {
            if (configFile.renameTo(backupFile)) {
                zAPI.getPlugin().getLogger().warning("Backed up broken config to: " + backupFile.getName());
            } else {
                zAPI.getPlugin().getLogger().warning("Failed to backup broken config file.");
            }
        } catch (Exception e) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Error while backing up config file", e);
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
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = zAPI.getPlugin().getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + zAPI.getColoredPluginName());
        }

        File outFile = new File(zAPI.getPlugin().getDataFolder(), resourcePath);
        File outDir = outFile.getParentFile();

        if (!outDir.exists() && !outDir.mkdirs()) {
            zAPI.getPlugin().getLogger().log(Level.WARNING, "Failed to create directory: " + outDir);
            return;
        }

        if (!outFile.exists() || replace) {
            try (OutputStream out = Files.newOutputStream(outFile.toPath())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                zAPI.getPlugin().getServer().getConsoleSender().sendMessage(zAPI.getColoredPluginName()
                        + "§eFile §f'" + resourcePath + "' §ehas been created.");
            } catch (IOException ex) {
                zAPI.getPlugin().getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
            }
        } else {
            zAPI.getPlugin().getLogger().log(Level.WARNING, "File " + outFile.getName() + " already exists and won't be replaced.");
        }
    }

}
