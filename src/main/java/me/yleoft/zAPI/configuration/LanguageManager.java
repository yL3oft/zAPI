package me.yleoft.zAPI.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LanguageManager class to manage language files.
 * It provides methods to load and manage language files.
 */
public class LanguageManager {

    private final File folder;
    private final String languageCode;
    private final String fallbackLanguageCode;
    private final Map<String, Object> options = new HashMap<>();
    private final Map<String, Object> fallbackOptions = new HashMap<>();
    private final File languageFile;
    private File fallbackFile;

    public LanguageManager(@NotNull File folder, @NotNull String languageCode) throws IOException {
        this(folder, languageCode, null);
    }

    /**
     * Creates a new LanguageManager instance.
     * @param folder The folder where the language files are located.
     * @param languageCode The language code to use.
     * @param fallbackLanguageCode The fallback language code to use.
     * @throws IOException If the language file cannot be found.
     */
    public LanguageManager(@NotNull File folder, @NotNull String languageCode, @Nullable String fallbackLanguageCode) throws IOException {
        this.folder = folder;
        this.languageCode = languageCode;
        this.fallbackLanguageCode = fallbackLanguageCode;

        this.languageFile = new File(folder, languageCode + ".yml");
        if (!languageFile.exists()) {
            throw new IOException("Language file not found: " + languageFile.getAbsolutePath());
        }

        if (fallbackLanguageCode != null) {
            this.fallbackFile = new File(folder, fallbackLanguageCode + ".yml");
            if (!fallbackFile.exists()) {
                throw new IOException("Fallback language file not found: " + fallbackFile.getAbsolutePath());
            }
        }

        load();
    }

    /**
     * Reloads the YAML files and repopulates the maps.
     */
    public void reload() throws IOException {
        options.clear();
        fallbackOptions.clear();
        load();
    }

    /**
     * Gets an option, checking main language first then fallback.
     * @param path The path to the option.
     * @return The option, or null if not found.
     */
    public Object get(@NotNull String path) {
        if (options.containsKey(path)) {
            return options.get(path);
        }
        if (fallbackLanguageCode != null && fallbackOptions.containsKey(path)) {
            return fallbackOptions.get(path);
        }
        return null;
    }

    /**
     * Gets an option as a string, checking main language first then fallback.
     * @param path The path to the option.
     * @return The option as a string, or null if not found.
     */
    public String getString(@NotNull String path) {
        Object value = get(path);
        return value != null ? value.toString() : null;
    }

    private void load() throws IOException {
        Yaml yaml;
        try {
            Class<?> loaderOptionsClass = Class.forName("org.yaml.snakeyaml.LoaderOptions");
            yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        }catch (ClassNotFoundException | NoSuchMethodError e) {
            yaml = new Yaml();
        }

        try (FileInputStream inputStream = new FileInputStream(languageFile)) {
            Object data = yaml.load(inputStream);
            if (data instanceof Map) {
                flattenMap("", (Map<?, ?>) data, options);
            }
        }

        if (fallbackFile != null) {
            try (FileInputStream inputStream = new FileInputStream(fallbackFile)) {
                Object data = yaml.load(inputStream);
                if (data instanceof Map) {
                    flattenMap("", (Map<?, ?>) data, fallbackOptions);
                }
            }
        }
    }

    private void flattenMap(String prefix, Map<?, ?> source, Map<String, Object> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;

            if (entry.getValue() instanceof Map) {
                flattenMap(fullPath, (Map<?, ?>) entry.getValue(), target);
            } else {
                target.put(fullPath, entry.getValue());
            }
        }
    }

    /**
     * Gets the current language code.
     * @return The current language code.
     */
    public String getCurrentLanguage() {
        return languageCode;
    }

    /**
     * Gets the fallback language code.
     * @return The fallback language code, or null if not set.
     */
    public String getFallbackLanguage() {
        return fallbackLanguageCode;
    }

    /**
     * Gets the folder where the language files are located.
     * @return The folder where the language files are located.
     */
    public File getFolder() {
        return folder;
    }
}
