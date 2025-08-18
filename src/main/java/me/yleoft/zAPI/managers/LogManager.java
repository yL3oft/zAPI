package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.utils.FileUtils;
import me.yleoft.zAPI.utils.LogUtils;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager class to manage log files.
 * It provides methods to create and manage log files.
 */
public class LogManager {

    private static final List<CustomLog> logFiles = new ArrayList<>();
    private static final File logFolder;

    static {
        logFolder = new File(zAPI.getPlugin().getDataFolder(), "logs");
        logFolder.mkdirs();
    }

    /**
     * Returns a list of all log utils loaded.
     */
    public static List<LogUtils> getFiles() {
        List<LogUtils> lus = new ArrayList<>();
        logFiles.forEach(cl -> lus.add(cl.getLu()));
        return lus;
    }

    /**
     * Creates a new file in the plugin's data folder.
     * @param name The path of the file to create.
     * @return The {@link FileUtils} of the created file or an existing file.
     */
    public static LogUtils createFile(@NotNull String name) {
        for (CustomLog cl : logFiles) {
            if (cl.getName().equals(name)) {
                return cl.getLu();
            }
        }
        CustomLog cl = new CustomLog(name);
        logFiles.add(cl);
        return cl.getLu();
    }

    /**
     * Retrieves a log utils by its name.
     * @param name The name of the file to retrieve.
     * @return The {@link YamlConfiguration} of the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public static LogUtils getLogUtil(@NotNull String name) {
        for (CustomLog cl : logFiles) {
            if (cl.getName().equals(name)) {
                return cl.getLu();
            }
        }
        throw new IllegalArgumentException("Unable to find log file: " + name);
    }

    /**
     * Retrieves a file by its name.
     * @param name The name of the file to retrieve.
     * @return The {@link YamlConfiguration} of the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public static File getFile(@NotNull String name) {
        return getLogUtil(name).getLogFile();
    }

    public static File getLogFolder() {
        return logFolder;
    }

    private static class CustomLog {
        private final LogUtils lu;
        private final String name;

        /**
         * Constructor to create a new custom file.
         * @param name The path of the file to create.
         */
        public CustomLog(@NotNull String name) {
            this.lu = new LogUtils(logFolder, name);
            this.name = name;
        }

        public File getFile() {
            return lu.getLogFile();
        }

        public LogUtils getLu() {
            return lu;
        }

        public String getName() {
            return name;
        }

    }
}
