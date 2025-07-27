package me.yleoft.zAPI.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.yleoft.zAPI.utils.ModrinthDownloader;
import me.yleoft.zAPI.zAPI;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * UpdateManager is a utility class for managing plugin updates.
 * It retrieves the latest version of the plugin from a specified URL
 * and provides methods to download and update the plugin.
 */
public class UpdateManager {

    private JavaPlugin plugin;
    private String url;
    private String id;

    public UpdateManager(JavaPlugin plugin, String url, String modrinthId) {
        this.plugin = plugin;
        this.url = url;
        this.id = modrinthId;
    }

    /**
     * Retrieves the latest version of the plugin from a specified URL.
     * @return The version string of the plugin, or the current plugin version if the URL retrieval fails.
     */
    public String getVersion() {
        try {
            String jsonString = readUrl(url);
            JsonArray json = (JsonArray) new JsonParser().parse(jsonString);
            JsonObject item = (JsonObject) json.get(0);
            return item.get("name").toString().replace("\"", "");
        }catch (Exception e) {
            e.printStackTrace();
        }

        return plugin.getDescription().getVersion();
    }

    /**
     * Reads the content of a URL and returns it as a String.
     *
     * @param urlString The URL to read from.
     * @return The content of the URL as a String.
     * @throws Exception If an error occurs while reading the URL.
     */
    @NotNull
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    /**
     * Updates the plugin by downloading the latest version from the specified URL.
     * The downloaded file is saved in the plugin's data folder with a versioned name.
     *
     * @return The path to the updated plugin JAR file, or "ERROR" if the update fails.
     */
    @NotNull
    public String update() {
        try {
            File file = locateJarInPlugins(plugin).toFile();
            file.delete();
            File directory = plugin.getDataFolder().getParentFile();
            String pluginName = plugin.getDescription().getName();

            String path = directory+"/"+pluginName+"-"+getVersion()+".jar";
            File newFile = new File(path);
            downloadFile(newFile, ModrinthDownloader.getLatestDownloadUrl(id));
            return path;
        } catch (Exception e) {}
        return "ERROR";
    }

    /**
     * Downloads a file from the specified URL to the given destination.
     *
     * @param destination The file to save the downloaded content to.
     * @param url         The URL to download the file from.
     */
    public static void downloadFile(File destination, String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
            connection.connect();
            FileOutputStream outputStream = new FileOutputStream(destination);
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int readBytes = 0;
            while ((readBytes = inputStream.read(buffer)) > 0)
                outputStream.write(buffer, 0, readBytes);
            outputStream.close();
            inputStream.close();
            connection.disconnect();
        } catch (Exception exception) {}
    }

    /**
     * Locates the original JAR file of the plugin in the plugins directory.
     * This is useful for updating or reloading the plugin from its original JAR.
     *
     * @param plugin The JavaPlugin instance of the plugin to locate.
     * @return The Path to the original JAR file, or null if not found.
     */
    public static Path locateJarInPlugins(JavaPlugin plugin) {
        Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
        String myMain = plugin.getDescription().getMain();
        String myName = plugin.getDescription().getName();
        String myVersion = plugin.getDescription().getVersion();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jarPath : stream) {
                try (JarFile jf = new JarFile(jarPath.toFile())) {
                    JarEntry entry = jf.getJarEntry("plugin.yml");
                    if (entry == null) continue;

                    try (InputStream in = jf.getInputStream(entry)) {
                        PluginDescriptionFile pdf = new PluginDescriptionFile(in);
                        if (myMain.equals(pdf.getMain())
                                || (myName.equalsIgnoreCase(pdf.getName()) && myVersion.equals(pdf.getVersion()))) {
                            return jarPath.toAbsolutePath();
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan plugins directory for the original JAR", e);
        }

        return null;
    }

}
