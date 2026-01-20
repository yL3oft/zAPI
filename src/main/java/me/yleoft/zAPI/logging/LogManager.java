package me.yleoft.zAPI.logging;

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
public abstract class LogManager {

    private static final List<CustomLog> logFiles = new ArrayList<>();
    private static final File logFolder;

    static {
        logFolder = new File(zAPI.getPlugin().getDataFolder(), "logs");
        logFolder.mkdirs();
    }

    /**
     * Returns a list of all log utils loaded.
     */
    public static List<FileLogger> getFiles() {
        List<FileLogger> lus = new ArrayList<>();
        logFiles.forEach(cl -> lus.add(cl.getLu()));
        return lus;
    }

    /**
     * Creates a new file in the plugin's data folder.
     * @param name The path of the file to create.
     * @return The {@link FileLogger} of the created file or an existing file.
     */
    public static FileLogger createFile(@NotNull String name) {
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
    public static FileLogger getLogUtil(@NotNull String name) {
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

    /**
     * Returns the log folder.
     */
    public static File getLogFolder() {
        return logFolder;
    }

    /**
     * CustomLog class to manage custom log files.
     * It provides methods to create and manage custom log files.
     */
    private static class CustomLog {
        private final FileLogger lu;
        private final String name;

        /**
         * Constructor to create a new custom file.
         * @param name The path of the file to create.
         */
        public CustomLog(@NotNull String name) {
            this.lu = new FileLogger(logFolder, name);
            this.name = name;
        }

        /**
         * Gets the log file.
         * @return The file.
         */
        public File getFile() {
            return lu.getLogFile();
        }

        /**
         * Gets the log utils.
         * @return The log utils.
         */
        public FileLogger getLu() {
            return lu;
        }

        /**
         * Gets the name of the log file.
         * @return The name of the log file.
         */
        public String getName() {
            return name;
        }

    }
}
