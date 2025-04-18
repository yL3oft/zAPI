package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.utils.FileUtils;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager class to manage files in the plugin's data folder.
 * It provides methods to create and manage language files.
 */
public abstract class FileManager {

    private static final List<CustomFile> files = new ArrayList<>();

    public static File df = zAPI.getPlugin().getDataFolder();

    public static List<FileUtils> getFiles() {
        List<FileUtils> fus = new ArrayList<>();
        files.forEach(cf -> fus.add(cf.getFu()));
        return fus;
    }

    /**
     * Creates a new file in the plugin's data folder.
     * @param path The path of the file to create.
     * @return The {@link YamlConfiguration} of the created file or an existing file.
     */
    public static FileUtils createFile(@NotNull String path) {
        for (CustomFile cf : files) {
            if (cf.getPath().equals(path)) {
                return cf.getFu();
            }
        }
        CustomFile cf = new CustomFile(path);
        files.add(cf);
        return cf.getFu();
    }

    /**
     * Retrieves a file utils by its name.
     * @param path The name of the file to retrieve.
     * @return The {@link YamlConfiguration} of the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public static FileUtils getFileUtil(@NotNull String path) {
        for (CustomFile cf : files) {
            if (cf.getPath().equals(path)) {
                return cf.getFu();
            }
        }
        throw new IllegalArgumentException("Unable to find file in: " + path);
    }

    /**
     * Retrieves a file by its name.
     * @param path The name of the file to retrieve.
     * @return The {@link YamlConfiguration} of the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public static YamlConfiguration getFile(@NotNull String path) {
        return (YamlConfiguration) getFileUtil(path).getConfig();
    }

    /**
     * Creates a custom file object.
     */
    private static class CustomFile {
        private final File file;
        private final FileUtils fu;
        private final String path;

        /**
         * Constructor to create a new custom file.
         * @param path The path of the file to create.
         */
        public CustomFile(@NotNull String path) {
            this.file = new File(df, path);
            this.fu = new FileUtils(file, path);
            this.path = path;
        }

        public File getFile() {
            return file;
        }

        public FileUtils getFu() {
            return fu;
        }

        public String getPath() {
            return path;
        }

    }

}
