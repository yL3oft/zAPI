package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.utils.FileUtils;
import me.yleoft.zAPI.zAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FileManager class to manage files in the plugin's data folder.
 * It provides methods to create and manage language files.
 */
public class FileManager {

    private zAPI zAPI;
    private ArrayList<CustomFile> files = new ArrayList<>();

    public File df;
    public File lang;
    public File lang2;
    public File fBACKUP = null;
    public FileUtils fuLang;
    public FileUtils fuLang2;
    public FileUtils fuBACKUP = null;

    /**
     * Constructor to initialize the FileManager with the zAPI instance.
     * It sets up the data folder and language files.
     * @param zAPI The zAPI instance.
     */
    public FileManager(@NotNull zAPI zAPI) {
        this.zAPI = zAPI;
        this.df = zAPI.getPlugin().getDataFolder();
        this.lang = new File(this.df, "languages/en.yml");
        this.lang2 = new File(this.df, "languages/pt-br.yml");
        this.fuLang = new FileUtils(zAPI, this.lang, "languages/en.yml");
        this.fuLang2 = new FileUtils(zAPI, this.lang2, "languages/pt-br.yml");
    }

    /**
     * Creates a new file in the plugin's data folder.
     * @param path The path of the file to create.
     * @return The {@link YamlConfiguration} of the created file or an existing file.
     */
    public YamlConfiguration createFile(@NotNull String path) {
        for (CustomFile cf : files) {
            if (cf.getName().equals(path)) {
                return (YamlConfiguration) cf.getFu().getConfig();
            }
        }
        CustomFile cf = new CustomFile(path);
        files.add(cf);
        return (YamlConfiguration) cf.getFu().getConfig();
    }

    /**
     * Retrieves a file by its name.
     * @param name The name of the file to retrieve.
     * @return The {@link YamlConfiguration} of the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public YamlConfiguration getFile(@NotNull String name) {
        for (CustomFile cf : files) {
            if (cf.getName().equals(name)) {
                return (YamlConfiguration) cf.getFu().getConfig();
            }
        }
        throw new IllegalArgumentException("Unable to find file with name: " + name);
    }

    /**
     * Creates a custom file object.
     */
    private class CustomFile {
        private File file;
        private FileUtils fu;
        private String name;

        /**
         * Constructor to create a new custom file.
         * @param path The path of the file to create.
         */
        public CustomFile(@NotNull String path) {
            this.file = new File(df, path+".yml");
            this.fu = new FileUtils(zAPI, file, path+".yml");
            this.name = file.getName();
        }

        public File getFile() {
            return file;
        }

        public FileUtils getFu() {
            return fu;
        }

        public String getName() {
            return name;
        }
    }

}
