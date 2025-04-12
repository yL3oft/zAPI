package me.yleoft.zAPI.utils;

import com.google.common.base.Charsets;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.logging.Level;

public class FileUtils {

    private zAPI zAPI;

    private FileConfiguration newConfig = null;
    private File configFile = null;
    private String resource = null;

    public FileUtils(zAPI zAPI, File f, String resource) {
        this.zAPI = zAPI;
        this.configFile = f;
        this.resource = resource;
    }

    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    public String getResource() {
        return resource;
    }

    public File getFile() {
        return configFile;
    }

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

    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            zAPI.getPlugin().getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource(resource, false);
        }
    }

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
