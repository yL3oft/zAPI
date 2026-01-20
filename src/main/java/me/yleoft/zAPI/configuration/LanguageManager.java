package me.yleoft.zAPI.configuration;

import me.yleoft.zAPI.zAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LanguageManager class to manage language files.
 * It provides methods to load and manage language files.
 */
public class LanguageManager {

    private final List<YAMLBuilder> languages;
    private final String languageCode;
    private final String fallbackLanguageCode;
    private final Map<String, Object> options = new HashMap<>();
    private final Map<String, Object> fallbackOptions = new HashMap<>();
    private final YAMLBuilder languageBuilder;
    private YAMLBuilder fallbackBuilder;

    /**
     * Creates a new LanguageManager instance.
     * @param languages The folder where the language files are located.
     * @param languageCode The language code to use.
     * @throws IOException If the language file cannot be found.
     */
    public LanguageManager(@NotNull List<YAMLBuilder> languages, @NotNull String languageCode) throws IOException {
        this(languages, languageCode, null);
    }

    /**
     * Creates a new LanguageManager instance.
     * @param languages The folder where the language files are located.
     * @param languageCode The language code to use.
     * @param fallbackLanguageCode The fallback language code to use.
     * @throws IOException If the language file cannot be found.
     */
    public LanguageManager(@NotNull List<YAMLBuilder> languages, @NotNull String languageCode, @Nullable String fallbackLanguageCode) throws IOException {
        this.languages = languages;
        this.languageCode = languageCode;
        this.fallbackLanguageCode = fallbackLanguageCode;

        this.languageBuilder = findLanguageBuilder(languageCode);
        if (languageBuilder == null) {
            zAPI.getLogger().warn("Language file not found for code: " + languageCode);
        }

        if (fallbackLanguageCode != null) {
            this.fallbackBuilder = findLanguageBuilder(fallbackLanguageCode);
            if (fallbackBuilder == null) {
                zAPI.getLogger().warn("Fallback language file not found for code: " + fallbackLanguageCode);
            }
        }

        load();
    }

    private YAMLBuilder findLanguageBuilder(String code) {
        for (YAMLBuilder builder : languages) {
            if (builder.getFile().getName().equals(code + ".yml")) {
                return builder;
            }
        }
        return null;
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

        try (FileInputStream inputStream = new FileInputStream(languageBuilder.getFile())) {
            Object data = yaml.load(inputStream);
            if (data instanceof Map) {
                flattenMap("", (Map<?, ?>) data, options);
            }
        }

        if (fallbackBuilder != null) {
            try (FileInputStream inputStream = new FileInputStream(fallbackBuilder.getFile())) {
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
    public List<YAMLBuilder> getLanguages() {
        return languages;
    }
}
