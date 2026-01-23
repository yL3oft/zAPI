package me.yleoft.zAPI.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * A utility class for building, editing, and managing YAML configuration files.
 * Features include:
 * - Default value management
 * - Comment support (headers, footers, inline comments)
 * - Version system with upgrade support
 * - Section organization with proper spacing
 */
public class YAMLBuilder extends Path {

    private final File file;
    private final Map<String, Object> defaults = new LinkedHashMap<>();
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, String[]> pendingComments = new LinkedHashMap<>();
    private final Map<String, String> pendingMoves = new LinkedHashMap<>();
    private final Map<String, Boolean> commentHighlight = new LinkedHashMap<>();
    private final Map<String, String[]> sectionComments = new LinkedHashMap<>();
    private final Set<String> voidedPaths = new HashSet<>();
    private String[] header = null;
    private String[] footer = null;
    private String[] nextComment = null;
    private boolean nextCommentHighlight = true;
    private String currentVersion = "1.0.0";
    private Map<String, Object> cachedData = null;

    private static final String VERSION_KEY = "config-version";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d)\\.(\\d{1,2})$");

    private boolean migrateLegacyColors = false;

    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("(?i)&x(&[0-9a-f]){6}");

    /**
     * Creates a new YAMLBuilder with a parent directory and file name.
     * Smart parsing:  if fileName doesn't end with .yml or .yaml, .yml is appended.
     *
     * @param parent   The parent directory
     * @param fileName The file name (with or without .yml extension)
     */
    public YAMLBuilder(File parent, String fileName) {
        if (! fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
            fileName = fileName + ".yml";
        }
        this.file = new File(parent, fileName);
        loadExistingVersion();
        loadCachedData();
    }

    /**
     * Creates a new YAMLBuilder with a specific file.
     *
     * @param file The configuration file
     */
    public YAMLBuilder(File file) {
        this.file = file;
        loadExistingVersion();
        loadCachedData();
    }

    /**
     * Loads the existing version from the file if it exists.
     */
    private void loadExistingVersion() {
        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(VERSION_KEY + ": ")) {
                        String version = trimmed.substring((VERSION_KEY + ":").length()).trim();
                        version = version.replace("\"", "").replace("'", "");
                        if (isValidVersion(version)) {
                            currentVersion = version;
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Silently ignore - use default version
            }
        }
    }

    /**
     * Loads and caches existing data from the file.
     */
    private void loadCachedData() {
        cachedData = loadExistingData();
    }

    /**
     * Refreshes the cached data from the file.
     *
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder refresh() {
        loadCachedData();
        loadExistingVersion();
        return this;
    }

    /**
     * Enable/disable migrating legacy Minecraft color codes (& / §) to MiniMessage tags during build().
     * This migrates BOTH defaults and any user-defined values loaded from disk.
     */
    public YAMLBuilder migrateLegacyColors(boolean enable) {
        this.migrateLegacyColors = enable;
        return this;
    }

    // ==================== GETTER METHODS ====================

    /**
     * Gets a String value from the config.
     *
     * @param path The path to the value
     * @return The String value, or null if not found
     */
    public String getString(String path) {
        Object value = getValue(path);
        if (value == null) return null;
        if (value instanceof MultiLineString) {
            return String.join("\n", ((MultiLineString) value).lines);
        }
        return value.toString();
    }

    /**
     * Gets a String value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The String value, or defaultValue if not found
     */
    public String getString(String path, String defaultValue) {
        String value = getString(path);
        return value != null ? value :  defaultValue;
    }

    /**
     * Gets a boolean value from the config.
     *
     * @param path The path to the value
     * @return The boolean value, or false if not found or not a boolean
     */
    public boolean getBoolean(String path) {
        Object value = getValue(path);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * Gets a boolean value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The boolean value, or defaultValue if not found
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("true")) return true;
            if (str.equalsIgnoreCase("false")) return false;
        }
        return defaultValue;
    }

    /**
     * Gets an int value from the config.
     *
     * @param path The path to the value
     * @return The int value, or 0 if not found or not a number
     */
    public int getInt(String path) {
        Object value = getValue(path);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Gets an int value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The int value, or defaultValue if not found
     */
    public int getInt(String path, int defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a long value from the config.
     *
     * @param path The path to the value
     * @return The long value, or 0 if not found or not a number
     */
    public long getLong(String path) {
        Object value = getValue(path);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * Gets a long value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The long value, or defaultValue if not found
     */
    public long getLong(String path, long defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a double value from the config.
     *
     * @param path The path to the value
     * @return The double value, or 0.0 if not found or not a number
     */
    public double getDouble(String path) {
        Object value = getValue(path);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Gets a double value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The double value, or defaultValue if not found
     */
    public double getDouble(String path, double defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a float value from the config.
     *
     * @param path The path to the value
     * @return The float value, or 0.0f if not found or not a number
     */
    public float getFloat(String path) {
        Object value = getValue(path);
        if (value == null) return 0.0f;
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return 0.0f;
            }
        }
        return 0.0f;
    }

    /**
     * Gets a float value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The float value, or defaultValue if not found
     */
    public float getFloat(String path, float defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a byte value from the config.
     *
     * @param path The path to the value
     * @return The byte value, or 0 if not found or not a number
     */
    public byte getByte(String path) {
        Object value = getValue(path);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).byteValue();
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Gets a byte value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The byte value, or defaultValue if not found
     */
    public byte getByte(String path, byte defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).byteValue();
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a short value from the config.
     *
     * @param path The path to the value
     * @return The short value, or 0 if not found or not a number
     */
    public short getShort(String path) {
        Object value = getValue(path);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).shortValue();
        if (value instanceof String) {
            try {
                return Short.parseShort((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Gets a short value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The short value, or defaultValue if not found
     */
    public short getShort(String path, short defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).shortValue();
        if (value instanceof String) {
            try {
                return Short.parseShort((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a char value from the config.
     *
     * @param path The path to the value
     * @return The char value, or '\0' if not found or empty
     */
    public char getChar(String path) {
        Object value = getValue(path);
        if (value == null) return '\0';
        if (value instanceof Character) return (Character) value;
        if (value instanceof String) {
            String str = (String) value;
            return str.isEmpty() ? '\0' : str.charAt(0);
        }
        return '\0';
    }

    /**
     * Gets a char value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The char value, or defaultValue if not found
     */
    public char getChar(String path, char defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof Character) return (Character) value;
        if (value instanceof String) {
            String str = (String) value;
            return str.isEmpty() ? defaultValue : str.charAt(0);
        }
        return defaultValue;
    }

    /**
     * Gets a List of Strings from the config.
     *
     * @param path The path to the value
     * @return The List of Strings, or an empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (value == null) return new ArrayList<>();
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Gets a List of Strings from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The List of Strings, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path, List<String> defaultValue) {
        Object value = getValue(path);
        if (value == null) return defaultValue;
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item != null ?  item.toString() : null);
            }
            return result;
        }
        return defaultValue;
    }

    /**
     * Gets a List of Integers from the config.
     *
     * @param path The path to the value
     * @return The List of Integers, or an empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<Integer> getIntList(String path) {
        Object value = getValue(path);
        if (value == null) return new ArrayList<>();
        if (value instanceof List) {
            List<Integer> result = new ArrayList<>();
            for (Object item :  (List<?>) value) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                } else if (item instanceof String) {
                    try {
                        result.add(Integer.parseInt((String) item));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Gets a List of Doubles from the config.
     *
     * @param path The path to the value
     * @return The List of Doubles, or an empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<Double> getDoubleList(String path) {
        Object value = getValue(path);
        if (value == null) return new ArrayList<>();
        if (value instanceof List) {
            List<Double> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Number) {
                    result.add(((Number) item).doubleValue());
                } else if (item instanceof String) {
                    try {
                        result.add(Double.parseDouble((String) item));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Gets the raw Object value from the config.
     *
     * @param path The path to the value
     * @return The Object value, or null if not found
     */
    public Object get(String path) {
        return getValue(path);
    }

    /**
     * Gets the raw Object value from the config with a default fallback.
     *
     * @param path         The path to the value
     * @param defaultValue The default value if not found
     * @return The Object value, or defaultValue if not found
     */
    public Object get(String path, Object defaultValue) {
        Object value = getValue(path);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if a path exists in the config.
     *
     * @param path The path to check
     * @return true if the path exists
     */
    public boolean contains(String path) {
        return getValue(path) != null;
    }

    /**
     * Checks if a path exists and is a specific type.
     *
     * @param path The path to check
     * @param type The expected type class
     * @return true if the path exists and is of the specified type
     */
    public boolean isType(String path, Class<?> type) {
        Object value = getValue(path);
        return value != null && type.isInstance(value);
    }

    /**
     * Internal method to get a value, checking cached data, then defaults.
     */
    private Object getValue(String path) {
        Object value = getValueFromPath(cachedData, path);
        if (value != null) return value;
        return defaults.get(path);
    }

    /**
     * Gets a configuration section at the specified path.
     * Returns a new YAMLSection that allows navigation within that section.
     *
     * @param path The path to the section
     * @return The YAMLSection, or null if the path doesn't exist or isn't a section
     */
    @SuppressWarnings("unchecked")
    public YAMLSection getConfigurationSection(String path) {
        Object value = getValue(path);
        if (value instanceof Map) {
            return new YAMLSection(this, path, (Map<String, Object>) value);
        }
        if (cachedData != null) {
            Object cached = getValueFromPath(cachedData, path);
            if (cached instanceof Map) {
                return new YAMLSection(this, path, (Map<String, Object>) cached);
            }
        }
        return null;
    }

    /**
     * Gets all keys at the root level or within a section.
     *
     * @param deep If true, returns all keys recursively with full paths
     * @return Set of keys
     */
    public Set<String> getKeys(boolean deep) {
        return getKeys("", deep);
    }

    /**
     * Gets all keys at a specific path.
     *
     * @param path The path to get keys from (empty string for root)
     * @param deep If true, returns all keys recursively with full paths
     * @return Set of keys
     */
    @SuppressWarnings("unchecked")
    public Set<String> getKeys(String path, boolean deep) {
        Set<String> keys = new LinkedHashSet<>();

        Map<String, Object> targetMap = cachedData;
        if (path != null && !path.isEmpty()) {
            Object value = getValueFromPath(cachedData, path);
            if (value instanceof Map) {
                targetMap = (Map<String, Object>) value;
            } else {
                return keys;
            }
        }

        if (targetMap == null) return keys;

        if (deep) {
            collectKeysDeep(targetMap, "", keys);
        } else {
            keys.addAll(targetMap.keySet());
        }

        return keys;
    }

    /**
     * Recursively collects all keys from a map.
     */
    @SuppressWarnings("unchecked")
    private void collectKeysDeep(Map<String, Object> map, String prefix, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(fullKey);
            if (entry.getValue() instanceof Map) {
                collectKeysDeep((Map<String, Object>) entry.getValue(), fullKey, keys);
            }
        }
    }

    // ==================== VOID PATH METHOD ====================

    /**
     * Completely erases an option from the config.
     * This removes it from the file if it exists and removes any default value.
     *
     * @param path The path to void
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder voidPath(String path) {
        // Remove from defaults
        defaults.remove(path);

        // Remove from values
        values.remove(path);

        // Remove from pending comments
        pendingComments.remove(path);
        commentHighlight.remove(path);

        // Remove from section comments
        sectionComments.remove(path);

        // Mark as voided (for build process)
        voidedPaths.add(path);

        // Remove from cached data
        if (cachedData != null) {
            removeValueFromPath(cachedData, path);
        }

        // Also remove any child paths (if this is a section)
        String pathPrefix = path + ".";
        defaults.entrySet().removeIf(e -> e.getKey().startsWith(pathPrefix));
        values.entrySet().removeIf(e -> e.getKey().startsWith(pathPrefix));
        pendingComments.entrySet().removeIf(e -> e.getKey().startsWith(pathPrefix));
        commentHighlight.entrySet().removeIf(e -> e.getKey().startsWith(pathPrefix));
        sectionComments.entrySet().removeIf(e -> e.getKey().startsWith(pathPrefix));

        return this;
    }

    /**
     * Checks if a path has been voided.
     *
     * @param path The path to check
     * @return true if the path has been voided
     */
    public boolean isVoided(String path) {
        if (voidedPaths.contains(path)) return true;
        // Check if any parent path is voided
        for (String voided : voidedPaths) {
            if (path.startsWith(voided + ".")) return true;
        }
        return false;
    }

    /**
     * Sets the header comments for the file.
     * Empty strings result in empty lines without #.
     *
     * @param lines The header lines
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder header(String... lines) {
        if (lines == null) {
            this.header = null;
            return this;
        }

        if (lines.length == 1 && isTextBox(lines[0])) {
            this.header = splitLinesPreserveEmpty(lines[0]);
        } else {
            this.header = lines;
        }
        return this;
    }

    /**
     * Sets the footer comments for the file.
     * Empty strings result in empty lines without #.
     * Footer appears before config-version.
     *
     * @param lines The footer lines
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder footer(String... lines) {
        if (lines == null) {
            this.footer = null;
            return this;
        }

        if (lines.length == 1 && isTextBox(lines[0])) {
            this.footer = splitLinesPreserveEmpty(lines[0]);
        } else {
            this.footer = lines;
        }
        return this;
    }

    /**
     * Adds a comment for the next option with highlight control.
     *
     * @param highlight Whether to highlight the comment
     * @param comments  The comment lines
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder comment(boolean highlight, String... comments) {
        if (comments != null && comments.length == 1 && isTextBox(comments[0])) {
            this.nextComment = splitLinesPreserveEmpty(comments[0]);
        } else {
            this.nextComment = comments;
        }
        this.nextCommentHighlight = highlight;
        return this;
    }

    /**
     * Adds a comment for the next option (highlighted by default).
     *
     * @param comments The comment lines
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder comment(String...comments) {
        return comment(true, comments);
    }

    /**
     * Adds a comment for a specific section.
     *
     * @param section  The section path
     * @param comments The comment lines
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder commentSection(String section, String... comments) {
        if (comments != null && comments.length == 1 && isTextBox(comments[0])) {
            sectionComments.put(section, splitLinesPreserveEmpty(comments[0]));
        } else {
            sectionComments.put(section, comments);
        }
        return this;
    }

    // ==================== ADD DEFAULT METHODS ====================

    /**
     * Adds a default String value.
     */
    public YAMLBuilder addDefault(String path, String value) {
        defaults.put(path, toYamlStringValue(value));
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default multi-line String value.
     */
    public YAMLBuilder addDefault(String path, String... values) {
        if (values.length == 1) {
            return addDefault(path, values[0]);
        }
        defaults.put(path, new MultiLineString(values));
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default int value.
     */
    public YAMLBuilder addDefault(String path, int value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default long value.
     */
    public YAMLBuilder addDefault(String path, long value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default double value.
     */
    public YAMLBuilder addDefault(String path, double value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default float value.
     */
    public YAMLBuilder addDefault(String path, float value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default boolean value.
     */
    public YAMLBuilder addDefault(String path, boolean value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default List of Strings value.
     */
    public YAMLBuilder addDefault(String path, List<String> value) {
        defaults.put(path, new ArrayList<>(value));
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default List of any type value.
     */
    public <T> YAMLBuilder addDefaultList(String path, List<T> value) {
        defaults.put(path, new ArrayList<>(value));
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default byte value.
     */
    public YAMLBuilder addDefault(String path, byte value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default short value.
     */
    public YAMLBuilder addDefault(String path, short value) {
        defaults.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Adds a default char value.
     */
    public YAMLBuilder addDefault(String path, char value) {
        defaults.put(path, String.valueOf(value));
        applyPendingComment(path);
        return this;
    }

    // ==================== SET VALUE METHODS ====================

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, String value) {
        values.put(path, toYamlStringValue(value));
        applyPendingComment(path);
        return this;
    }

    public YAMLBuilder setValue(String path, String... values) {
        if (values.length == 1) {
            return setValue(path, values[0]);
        }
        this.values.put(path, new MultiLineString(values));
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, int value) {
        values.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, double value) {
        values.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, boolean value) {
        values.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, List<String> value) {
        values.put(path, new ArrayList<>(value));
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, long value) {
        values.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Sets a value (non-default - only written if file is being generated).
     */
    public YAMLBuilder setValue(String path, float value) {
        values.put(path, value);
        applyPendingComment(path);
        return this;
    }

    /**
     * Applies the pending comment to the given path.
     */
    private void applyPendingComment(String path) {
        if (nextComment != null) {
            pendingComments.put(path, nextComment);
            commentHighlight.put(path, nextCommentHighlight);
            nextComment = null;
            nextCommentHighlight = true;
        }
    }

    // ==================== RUNTIME SET (POST-BUILD) METHODS ====================

    /**
     * Sets a value in the ACTUAL config (cachedData) and saves to disk immediately.
     * This is the "natural YAML" setter behavior (works after build()).
     *
     * If value is null -> removes the path.
     */
    public YAMLBuilder set(String path, Object value) {
        // Ensure cache is loaded
        if (cachedData == null) {
            cachedData = new LinkedHashMap<>();
        }

        // Respect voiding: if voided, ignore (or you could unvoid; current behavior: ignore)
        if (isVoided(path)) {
            return this;
        }

        // Apply pending moves before writing anything so we don't resurrect old keys
        if (!pendingMoves.isEmpty()) {
            applyPendingMoves();
        }

        if (value == null) {
            removeValueFromPath(cachedData, path);
        } else {
            // Normalize multiline strings into MultiLineString
            Object yamlValue = value;

            if (value instanceof String s) {
                yamlValue = toYamlStringValue(s);
            } else if (value instanceof String[] arr) {
                yamlValue = (arr.length <= 1) ? toYamlStringValue(arr.length == 0 ? "" : arr[0]) : new MultiLineString(arr);
            } else if (value instanceof List<?> list) {
                // Keep list, but normalize each string entry if color migration is enabled (optional)
                yamlValue = new ArrayList<>(list);
            }

            setValueAtPath(cachedData, path, yamlValue);
        }

        // Optionally migrate colors on write for runtime sets too
        if (migrateLegacyColors) {
            convertLegacyColorsInObject(cachedData);
        }

        // Remove any voided paths that may exist in cache
        for (String voidedPath : voidedPaths) {
            removeValueFromPath(cachedData, voidedPath);
        }

        // Write updated cache
        try {
            writeYamlFile(cachedData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Refresh internal state
        loadExistingVersion();
        loadCachedData();

        return this;
    }

    /** Same as set(path, null) */
    public YAMLBuilder unset(String path) {
        return set(path, null);
    }

    public YAMLBuilder setString(String path, String value) {
        return set(path, value);
    }

    public YAMLBuilder setBoolean(String path, boolean value) {
        return set(path, value);
    }

    public YAMLBuilder setInt(String path, int value) {
        return set(path, value);
    }

    /** Alias for people asking for "setInteger" */
    public YAMLBuilder setInteger(String path, int value) {
        return setInt(path, value);
    }

    public YAMLBuilder setLong(String path, long value) {
        return set(path, value);
    }

    public YAMLBuilder setDouble(String path, double value) {
        return set(path, value);
    }

    public YAMLBuilder setFloat(String path, float value) {
        return set(path, value);
    }

    public YAMLBuilder setStringList(String path, List<String> value) {
        return set(path, value == null ? null : new ArrayList<>(value));
    }

    public YAMLBuilder setList(String path, List<?> value) {
        return set(path, value == null ? null : new ArrayList<>(value));
    }

    /**
     * Sets a multi-line string block (|) at the given path.
     * Each entry is one line.
     */
    public YAMLBuilder setMultiline(String path, String... lines) {
        if (lines == null) return set(path, null);
        if (lines.length <= 1) return set(path, lines.length == 0 ? "" : lines[0]);
        return set(path, new MultiLineString(lines));
    }

    // ==================== FILECONFIG METHOD ====================

    /**
     * Gets a Bukkit FileConfiguration representation of this config.
     * Note: This loads the file fresh from disk - changes made through
     * this FileConfiguration won't be reflected in the YAMLBuilder.
     *
     * @return The FileConfiguration, or an empty configuration if the file doesn't exist
     */
    public FileConfiguration getConfig() {
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        return new YamlConfiguration();
    }

    // ==================== VERSION METHODS ====================

    /**
     * Returns the current version of the config.
     *
     * @return The current version string
     */
    public String version() {
        return currentVersion;
    }

    /**
     * Updates the version of the config.
     * Only updates if the new version is newer than the current version.
     *
     * IMPORTANT: This should also work before the file exists (first build),
     * otherwise updateVersion() can't be used when generating a config for the first time.
     *
     * @param newVersion The new version string
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder updateVersion(String newVersion) {
        if (isValidVersion(newVersion) && compareVersions(newVersion, currentVersion) > 0) {
            currentVersion = newVersion;
        }
        return this;
    }

    /**
     * Checks if the current version is newer than the specified version.
     *
     * @param version The version to compare against
     * @return true if current version is newer
     */
    public boolean versionNewer(String version) {
        return compareVersions(currentVersion, version) > 0;
    }

    /**
     * Checks if the current version is older than the specified version.
     *
     * @param version The version to compare against
     * @return true if current version is older
     */
    public boolean versionOlder(String version) {
        return compareVersions(currentVersion, version) < 0;
    }

    /**
     * Checks if the current version is equal to the specified version.
     *
     * @param version The version to compare against
     * @return true if versions are equal
     */
    public boolean versionEqual(String version) {
        return compareVersions(currentVersion, version) == 0;
    }

    /**
     * Validates a version string format.
     */
    private boolean isValidVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return false;
        }
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        return minor <= 9 && patch <= 99;
    }

    /**
     * Compares two version strings.
     *
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private int compareVersions(String v1, String v2) {
        if (! isValidVersion(v1) || !isValidVersion(v2)) {
            return 0;
        }
        Matcher m1 = VERSION_PATTERN.matcher(v1);
        Matcher m2 = VERSION_PATTERN.matcher(v2);
        m1.matches();
        m2.matches();

        int major1 = Integer.parseInt(m1.group(1));
        int minor1 = Integer.parseInt(m1.group(2));
        int patch1 = Integer.parseInt(m1.group(3));

        int major2 = Integer.parseInt(m2.group(1));
        int minor2 = Integer.parseInt(m2.group(2));
        int patch2 = Integer.parseInt(m2.group(3));

        if (major1 != major2) return major1 - major2;
        if (minor1 != minor2) return minor1 - minor2;
        return patch1 - patch2;
    }

    // ==================== MOVE METHOD ====================

    /**
     * Moves a section or value from one path to another.
     * If the source path doesn't exist, this is ignored.
     *
     * @param path    The source path
     * @param newPath The destination path
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder move(String path, String newPath) {
        pendingMoves.put(path, newPath);
        return this;
    }

    /**
     * Applies all pending moves to the cached data and defaults.
     */
    private void applyPendingMoves() {
        for (Map.Entry<String, String> moveEntry : pendingMoves.entrySet()) {
            String fromPath = moveEntry.getKey();
            String toPath = moveEntry.getValue();
            String pathPrefix = fromPath + ".";
            String newPathPrefix = toPath + ".";

            // === Move in cachedData ===
            if (cachedData != null) {
                Object value = getValueFromPath(cachedData, fromPath);
                if (value != null) {
                    removeValueFromPath(cachedData, fromPath);
                    setValueAtPath(cachedData, toPath, value);
                }
            }

            // === Move in defaults ===
            // Move exact match
            if (defaults.containsKey(fromPath)) {
                defaults.put(toPath, defaults.remove(fromPath));
            }

            // Move children
            Map<String, Object> defaultsToAdd = new LinkedHashMap<>();
            Set<String> defaultsToRemove = new HashSet<>();
            for (String key : defaults.keySet()) {
                if (key.startsWith(pathPrefix)) {
                    String newKey = newPathPrefix + key.substring(pathPrefix.length());
                    defaultsToAdd.put(newKey, defaults.get(key));
                    defaultsToRemove.add(key);
                }
            }
            for (String key : defaultsToRemove) {
                defaults.remove(key);
            }
            defaults.putAll(defaultsToAdd);

            // === Move in values ===
            if (values.containsKey(fromPath)) {
                values.put(toPath, values.remove(fromPath));
            }

            Map<String, Object> valuesToAdd = new LinkedHashMap<>();
            Set<String> valuesToRemove = new HashSet<>();
            for (String key :  values.keySet()) {
                if (key.startsWith(pathPrefix)) {
                    String newKey = newPathPrefix + key.substring(pathPrefix.length());
                    valuesToAdd.put(newKey, values.get(key));
                    valuesToRemove.add(key);
                }
            }
            for (String key : valuesToRemove) {
                values.remove(key);
            }
            values.putAll(valuesToAdd);

            // === Move pendingComments ===
            if (pendingComments.containsKey(fromPath)) {
                pendingComments.put(toPath, pendingComments.remove(fromPath));
            }

            Map<String, String[]> commentsToAdd = new LinkedHashMap<>();
            Set<String> commentsToRemove = new HashSet<>();
            for (String key : pendingComments.keySet()) {
                if (key.startsWith(pathPrefix)) {
                    String newKey = newPathPrefix + key.substring(pathPrefix.length());
                    commentsToAdd.put(newKey, pendingComments.get(key));
                    commentsToRemove.add(key);
                }
            }
            for (String key : commentsToRemove) {
                pendingComments.remove(key);
            }
            pendingComments.putAll(commentsToAdd);

            // === Move commentHighlight ===
            if (commentHighlight.containsKey(fromPath)) {
                commentHighlight.put(toPath, commentHighlight.remove(fromPath));
            }

            Map<String, Boolean> highlightsToAdd = new LinkedHashMap<>();
            Set<String> highlightsToRemove = new HashSet<>();
            for (String key : commentHighlight.keySet()) {
                if (key.startsWith(pathPrefix)) {
                    String newKey = newPathPrefix + key.substring(pathPrefix.length());
                    highlightsToAdd.put(newKey, commentHighlight.get(key));
                    highlightsToRemove.add(key);
                }
            }
            for (String key :  highlightsToRemove) {
                commentHighlight.remove(key);
            }
            commentHighlight.putAll(highlightsToAdd);

            // === Move sectionComments ===
            if (sectionComments.containsKey(fromPath)) {
                sectionComments.put(toPath, sectionComments.remove(fromPath));
            }

            Map<String, String[]> sectionCommentsToAdd = new LinkedHashMap<>();
            Set<String> sectionCommentsToRemove = new HashSet<>();
            for (String key : sectionComments.keySet()) {
                if (key.startsWith(pathPrefix)) {
                    String newKey = newPathPrefix + key.substring(pathPrefix.length());
                    sectionCommentsToAdd.put(newKey, sectionComments.get(key));
                    sectionCommentsToRemove.add(key);
                }
            }
            for (String key : sectionCommentsToRemove) {
                sectionComments.remove(key);
            }
            sectionComments.putAll(sectionCommentsToAdd);
        }

        // Clear pending moves after applying
        pendingMoves.clear();
    }

    // ==================== BUILD METHOD ====================

    /**
     * Builds and writes the YAML file.
     * This method will not throw exceptions - errors are handled silently.
     *
     * @return This YAMLBuilder for chaining
     */
    public YAMLBuilder build() {
        try {
            boolean firstTimeGenerate = ! file.exists();

            // Ensure parent directories exist
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // APPLY PENDING MOVES FIRST - before anything else!
            applyPendingMoves();

            // Now merge:  start with cachedData (which has moves applied)
            Map<String, Object> finalData = new LinkedHashMap<>();

            // First, copy all existing/cached data (preserves user values AND moved values)
            if (cachedData != null) {
                copyAllValues(cachedData, finalData, "");
            }

            // Combine defaults and values in declaration order
            // We need to maintain the order in which paths were declared
            Map<String, Object> allEntries = new LinkedHashMap<>();
            allEntries.putAll(defaults);

            if (firstTimeGenerate) {
                // For first-time generation, merge values into allEntries
                // We need to insert values at the correct position relative to defaults
                allEntries = mergeInDeclarationOrder(defaults, values);
            }

            // Now add them all in order
            for (Map.Entry<String, Object> entry : allEntries.entrySet()) {
                String path = entry.getKey();

                if (isVoided(path)) continue;

                Object existingValue = getValueFromPath(finalData, path);
                if (existingValue == null) {
                    setValueAtPathOrdered(finalData, path, entry.getValue(), allEntries);
                }
            }

            // Remove voided paths
            for (String voidedPath : voidedPaths) {
                removeValueFromPath(finalData, voidedPath);
            }

            if (migrateLegacyColors) {
                convertLegacyColorsInObject(finalData);
            }

            writeYamlFile(finalData);

            loadCachedData();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Merges defaults and values maps while preserving declaration order.
     * Values are inserted at positions that maintain proper YAML structure.
     */
    private Map<String, Object> mergeInDeclarationOrder(Map<String, Object> defaults, Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> processedValues = new HashSet<>();

        for (Map.Entry<String, Object> defaultEntry : defaults.entrySet()) {
            String defaultPath = defaultEntry.getKey();

            // Before adding this default, check if there are any values that should come before it
            // (values that share the same parent but weren't in defaults before this point)
            String defaultParent = getParentPath(defaultPath);

            for (Map.Entry<String, Object> valueEntry : values.entrySet()) {
                String valuePath = valueEntry.getKey();
                if (processedValues.contains(valuePath)) continue;

                String valueParent = getParentPath(valuePath);

                // If this value has the same parent as the current default,
                // and this value's key comes before any nested sections in the default
                if (valueParent.equals(defaultParent) && shouldInsertValueBefore(valuePath, defaultPath, defaults)) {
                    result.put(valuePath, valueEntry.getValue());
                    processedValues.add(valuePath);
                }
            }

            result.put(defaultPath, defaultEntry.getValue());
        }

        // Add any remaining values that weren't processed
        for (Map.Entry<String, Object> valueEntry : values.entrySet()) {
            if (!processedValues.contains(valueEntry.getKey()) && !isVoided(valueEntry.getKey())) {
                result.put(valueEntry.getKey(), valueEntry.getValue());
            }
        }

        return result;
    }

    /**
     * Determines if a value path should be inserted before a default path.
     * This happens when the value is a sibling scalar and the default starts a nested section.
     */
    private boolean shouldInsertValueBefore(String valuePath, String defaultPath, Map<String, Object> defaults) {
        String valueParent = getParentPath(valuePath);
        String defaultParent = getParentPath(defaultPath);

        // Only consider if they share the same parent
        if (!valueParent.equals(defaultParent)) {
            return false;
        }

        // Check if the defaultPath starts a nested section (has children in defaults)
        String defaultPathPrefix = defaultPath + ".";
        for (String key : defaults.keySet()) {
            if (key.startsWith(defaultPathPrefix)) {
                // defaultPath is a section, value should come before if it's a scalar sibling
                // that would logically appear before nested sections
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the parent path of a dotted path.
     */
    private String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : "";
    }

    /**
     * Sets a value at a path while maintaining proper key ordering within each level.
     * This ensures that keys are inserted in the order they appear in allEntries.
     */
    @SuppressWarnings("unchecked")
    private void setValueAtPathOrdered(Map<String, Object> data, String path, Object value, Map<String, Object> allEntries) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (! current.containsKey(part) || !(current.get(part) instanceof Map)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(part);
        }

        String finalKey = parts[parts.length - 1];

        // If the key already exists, just update it
        if (current.containsKey(finalKey)) {
            current.put(finalKey, value);
            return;
        }

        // Otherwise, we need to insert it in the correct order
        // Find all sibling paths and determine the correct insertion order
        String parentPath = getParentPath(path);
        List<String> siblingOrder = new ArrayList<>();

        for (String entryPath : allEntries.keySet()) {
            String entryParent = getParentPath(entryPath);
            if (entryParent.equals(parentPath)) {
                String siblingKey = entryPath.substring(parentPath.isEmpty() ? 0 : parentPath.length() + 1);
                if (siblingKey.contains(".")) {
                    siblingKey = siblingKey.substring(0, siblingKey.indexOf('.'));
                }
                if (!siblingOrder.contains(siblingKey)) {
                    siblingOrder.add(siblingKey);
                }
            }
        }

        // Rebuild the map in the correct order
        Map<String, Object> reordered = new LinkedHashMap<>();
        Set<String> existingKeys = new LinkedHashSet<>(current.keySet());
        existingKeys.add(finalKey);

        // First add keys in the order they appear in siblingOrder
        for (String orderedKey : siblingOrder) {
            if (existingKeys.contains(orderedKey)) {
                if (orderedKey.equals(finalKey)) {
                    reordered.put(finalKey, value);
                } else if (current.containsKey(orderedKey)) {
                    reordered.put(orderedKey, current.get(orderedKey));
                }
            }
        }

        // Then add any remaining keys that weren't in siblingOrder
        for (String existingKey : existingKeys) {
            if (!reordered.containsKey(existingKey)) {
                if (existingKey.equals(finalKey)) {
                    reordered.put(finalKey, value);
                } else if (current.containsKey(existingKey)) {
                    reordered.put(existingKey, current.get(existingKey));
                }
            }
        }

        // Replace the contents of current with reordered
        current.clear();
        current.putAll(reordered);
    }

    private static boolean isTextBox(String s) {
        return s != null && (s.contains("\n") || s.contains("\r"));
    }

    private static String[] splitLinesPreserveEmpty(String s) {
        if (s == null) return new String[0];

        String normalized = s.replace("\r\n", "\n").replace('\r', '\n');

        boolean endsWithNewline = !normalized.isEmpty() && normalized.charAt(normalized.length() - 1) == '\n';

        String[] parts = normalized.split("\n", -1);

        if (endsWithNewline && parts.length > 0 && parts[parts.length - 1].isEmpty()) {
            parts = Arrays.copyOf(parts, parts.length - 1);
        }

        return parts;
    }

    /**
     * Converts a String to a YAML-compatible value.
     * If the string contains newlines, it is converted to a MultiLineString.
     *
     * @param s The input string
     * @return The original string or a MultiLineString for multi-line content
     */
    private static Object toYamlStringValue(String s) {
        if (s == null) return null;
        if (!isTextBox(s)) return s;

        String[] lines = splitLinesPreserveEmpty(s);
        // If it ended up as 1 line anyway, keep it scalar
        if (lines.length <= 1) return s;

        return new MultiLineString(lines);
    }

    @SuppressWarnings("unchecked")
    private void convertLegacyColorsInObject(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            // iterate over a snapshot to avoid concurrent modification issues
            for (Map.Entry<?, ?> entry : new ArrayList<>(map.entrySet())) {
                Object key = entry.getKey();
                Object v = entry.getValue();

                if (v instanceof String s) {
                    ((Map<Object, Object>) map).put(key, legacyToMiniMessage(s));
                } else if (v instanceof MultiLineString mls) {
                    String[] out = new String[mls.lines.length];
                    for (int i = 0; i < mls.lines.length; i++) out[i] = legacyToMiniMessage(mls.lines[i]);
                    ((Map<Object, Object>) map).put(key, new MultiLineString(out));
                } else if (v instanceof List<?> list) {
                    List<Object> newList = new ArrayList<>(list.size());
                    for (Object item : list) {
                        if (item instanceof String sItem) newList.add(legacyToMiniMessage(sItem));
                        else newList.add(item);
                    }
                    ((Map<Object, Object>) map).put(key, newList);
                } else {
                    convertLegacyColorsInObject(v);
                }
            }
        }
    }

    private String legacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) return input;

        // normalize § to &
        String s = input.replace('§', '&');

        // &#RRGGBB or #RRGGBB -> <#RRGGBB>
        s = AMP_HEX_PATTERN.matcher(s).replaceAll("<#$1>");

        // &x&F&F&0&0&0&0 -> <#FF0000>
        Matcher m = SPIGOT_HEX_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group();
            String hex = token.replace("&x", "").replace("&", "");
            m.appendReplacement(sb, "<#" + hex + ">");
        }
        m.appendTail(sb);
        s = sb.toString();

        // legacy colors + formats -> MiniMessage tags
        return s
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    /**
     * Recursively copies all values from source to destination map.
     * This preserves user-added values that aren't in defaults.
     */
    @SuppressWarnings("unchecked")
    private void copyAllValues(Map<String, Object> source, Map<String, Object> dest, String pathPrefix) {
        if (source == null) return;

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;

            // Skip the version key - it's handled separately
            if (key.equals(VERSION_KEY) && pathPrefix.isEmpty()) continue;

            // Skip voided paths
            if (isVoided(fullPath)) continue;

            if (value instanceof Map) {
                // Ensure the nested map exists in dest
                if (! dest.containsKey(key)) {
                    dest.put(key, new LinkedHashMap<String, Object>());
                }
                Object destValue = dest.get(key);
                if (destValue instanceof Map) {
                    copyAllValues((Map<String, Object>) value, (Map<String, Object>) destValue, fullPath);
                }
            } else {
                // Copy the value directly
                dest.put(key, value);
            }
        }
    }

    /**
     * Loads existing data from the file.
     */
    private Map<String, Object> loadExistingData() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (! file.exists()) {
            return data;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            parseYaml(lines, data);
        } catch (Exception e) {
            // Return empty map on error
        }
        return data;
    }

    /**
     * Simple YAML parser for reading existing values.
     */
    private void parseYaml(List<String> lines, Map<String, Object> data) {
        Deque<String> pathStack = new ArrayDeque<>();
        Deque<Integer> indentStack = new ArrayDeque<>();
        indentStack.push(-1);

        String currentListPath = null;
        List<String> currentList = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Skip comments and empty lines
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            int indent = getIndent(line);
            String trimmed = line.trim();

            // Handle list items
            if (trimmed.startsWith("- ")) {
                if (currentListPath != null && currentList != null) {
                    String value = trimmed.substring(2).trim();
                    value = unquote(value);
                    currentList.add(value);
                }
                continue;
            } else if (currentListPath != null) {
                // End of list
                setValueAtPath(data, currentListPath, currentList);
                currentListPath = null;
                currentList = null;
            }

            // Adjust path based on indent
            while (!indentStack.isEmpty() && indent <= indentStack.peek()) {
                indentStack.pop();
                if (!pathStack.isEmpty()) pathStack.pop();
            }

            // Parse key-value
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) continue;

            String key = trimmed.substring(0, colonIndex).trim();
            String valueStr = trimmed.substring(colonIndex + 1).trim();

            // Push current key context
            pathStack.push(key);
            indentStack.push(indent);

            String fullPath = buildFullPathFromStack(pathStack);

            if (valueStr.isEmpty()) {
                String next = nextMeaningfulLine(lines, i + 1);

                if (next != null) {
                    int nextIndent = getIndent(next);
                    String nextTrimmed = next.trim();

                    if (nextIndent > indent) {
                        // It's a real section (or a list section).Keep it on the stack.
                        if (nextTrimmed.startsWith("- ")) {
                            currentListPath = fullPath;
                            currentList = new ArrayList<>();
                        }
                        continue;
                    }
                }

                // Not actually a section with children -> undo the push
                pathStack.pop();
                indentStack.pop();
                continue;
            }

            if (valueStr.equals("|")) {
                int baseIndent = indent;

                List<String> blockLines = new ArrayList<>();
                int j = i + 1;

                Integer blockIndent = null;

                while (j < lines.size()) {
                    String nextLine = lines.get(j);

                    // If we hit a blank line, only treat it as part of the block if it is indented
                    // deeper than the base indent.Otherwise, it's a separator and ends the block.
                    if (nextLine.trim().isEmpty()) {
                        int nextIndent = getIndent(nextLine);
                        if (nextIndent <= baseIndent) {
                            break; // separator line, not part of the block
                        }

                        // Still inside the block -> keep an empty line
                        blockLines.add("");
                        j++;
                        continue;
                    }

                    int nextIndent = getIndent(nextLine);

                    // Block ends when indentation returns to base level or less
                    if (nextIndent <= baseIndent) {
                        break;
                    }

                    if (blockIndent == null) {
                        blockIndent = nextIndent;
                    }

                    blockLines.add(stripIndent(nextLine, blockIndent));
                    j++;
                }

                Object value;
                if (blockLines.isEmpty()) {
                    value = "";
                } else {
                    value = new MultiLineString(blockLines.toArray(new String[0]));
                }

                setValueAtPath(data, fullPath, value);

                // IMPORTANT: pop because this key is done (same as scalar case)
                pathStack.pop();
                indentStack.pop();

                i = j - 1;
                continue;
            }

            Object value = parseValue(valueStr);
            setValueAtPath(data, fullPath, value);

            // Pop after scalar assignment (same as your original)
            pathStack.pop();
            indentStack.pop();
        }

        if (currentListPath != null && currentList != null && !currentList.isEmpty()) {
            setValueAtPath(data, currentListPath, currentList);
        }
    }

    private String buildFullPathFromStack(Deque<String> pathStack) {
        String[] parts = pathStack.toArray(new String[0]);
        StringBuilder reversedPath = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (reversedPath.length() > 0) reversedPath.append(".");
            reversedPath.append(parts[i]);
        }
        return reversedPath.toString();
    }

    private String stripIndent(String line, int indentToStrip) {
        int stripped = 0;
        int idx = 0;

        while (idx < line.length() && stripped < indentToStrip) {
            char c = line.charAt(idx);
            if (c == ' ') {
                stripped++;
                idx++;
            } else if (c == '\t') {
                // you treat tab as 2 spaces in getIndent
                stripped += 2;
                idx++;
            } else {
                break;
            }
        }

        return idx <= line.length() ? line.substring(idx) : line.trim();
    }

    /**
     * Parses a YAML value string into appropriate type.
     */
    private Object parseValue(String value) {
        value = value.trim();

        // Remove quotes
        value = unquote(value);

        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {
        }

        // Boolean
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;

        return value;
    }

    /**
     * Removes surrounding quotes from a string.
     */
    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Gets the indentation level of a line.
     */
    private int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 2; // Treat tab as 2 spaces
            else break;
        }
        return count;
    }

    /**
     * Gets a value from a nested map using dot notation path.
     */
    @SuppressWarnings("unchecked")
    private Object getValueFromPath(Map<String, Object> data, String path) {
        if (data == null) return null;
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) return null;
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * Sets a value in a nested map using dot notation path.
     */
    @SuppressWarnings("unchecked")
    private void setValueAtPath(Map<String, Object> data, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (! current.containsKey(part) || !(current.get(part) instanceof Map)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(part);
        }

        current.put(parts[parts.length - 1], value);
    }

    /**
     * Removes a value from a nested map using dot notation path.
     */
    @SuppressWarnings("unchecked")
    private void removeValueFromPath(Map<String, Object> data, String path) {
        if (data == null) return;
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return;
            }
        }

        current.remove(parts[parts.length - 1]);

        // Clean up empty parent sections
        cleanupEmptyParents(data, path);
    }

    /**
     * Removes empty parent sections after a path has been removed.
     */
    @SuppressWarnings("unchecked")
    private void cleanupEmptyParents(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        for (int i = parts.length - 2; i >= 0; i--) {
            String parentPath = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            Object parent = getValueFromPath(data, parentPath);
            if (parent instanceof Map && ((Map<?, ?>) parent).isEmpty()) {
                removeValueFromPath(data, parentPath);
            } else {
                break;
            }
        }
    }

    /**
     * Finds the next meaningful line (non-empty, non-comment) from a list of lines.
     */
    private static String nextMeaningfulLine(List<String> lines, int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            String l = lines.get(i);
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            return l;
        }
        return null;
    }

    /**
     * Writes the YAML file with proper formatting.
     */
    private void writeYamlFile(Map<String, Object> data) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Write header
        if (header != null && header.length > 0) {
            for (String line : header) {
                if (line.isEmpty()) {
                    sb.append("\n");
                } else if (line.startsWith("#")) {
                    sb.append(line).append("\n");
                } else {
                    sb.append("# ").append(line).append("\n");
                }
            }
            sb.append("\n");
        }

        // Track which paths have been written for comments
        Set<String> writtenPaths = new HashSet<>();

        // Write data
        writeMap(sb, data, 0, "", null, writtenPaths);

        // Write footer
        if (footer != null && footer.length > 0) {
            sb.append("\n");
            for (String line : footer) {
                if (line.isEmpty()) {
                    sb.append("\n");
                } else {
                    sb.append("# ").append(line).append("\n");
                }
            }
        }

        // Write version (always at bottom with empty line above)
        sb.append("\n");
        sb.append(VERSION_KEY).append(": \"").append(currentVersion).append("\"\n");

        // Write to file
        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recursively writes a map to the StringBuilder with proper YAML formatting.
     */
    @SuppressWarnings("unchecked")
    private void writeMap(StringBuilder sb, Map<String, Object> map, int indent,
                          String pathPrefix, String previousTopSection, Set<String> writtenPaths) {
        String indentStr = repeat("  ", indent);
        boolean isTopLevel = indent == 0;
        String lastTopSection = previousTopSection;

        for (Map.Entry<String, Object> entry :  map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            String currentTopSection = isTopLevel ? key : (pathPrefix.isEmpty() ? key : pathPrefix.split("\\.")[0]);

            // Skip voided paths
            if (isVoided(fullPath)) continue;

            // Add blank line between different top-level sections
            if (isTopLevel && lastTopSection != null && !lastTopSection.equals(key)) {
                sb.append("\n");
            }

            // Check for section comment
            if (sectionComments.containsKey(fullPath)) {
                if (sb.length() > 0 && ! sb.toString().endsWith("\n\n")) {
                    sb.append("\n");
                }
                for (String comment : sectionComments.get(fullPath)) {
                    sb.append(indentStr).append("# ").append(comment).append("\n");
                }
            }

            if (pendingComments.containsKey(fullPath)) {
                String[] comments = pendingComments.get(fullPath);

                boolean highlight = Boolean.TRUE.equals(commentHighlight.get(fullPath));

                if (sb.length() > 0) {
                    if (!sb.toString().endsWith("\n")) {
                        sb.append("\n");
                    }

                    if (highlight && !sb.toString().endsWith("\n\n")) {
                        sb.append("\n");
                    }
                }

                for (String comment : comments) {
                    sb.append(indentStr).append("# ").append(comment).append("\n");
                }
            }

            if (value instanceof Map) {
                sb.append(indentStr).append(key).append(":\n");
                writeMap(sb, (Map<String, Object>) value, indent + 1, fullPath, currentTopSection, writtenPaths);
            } else if (value instanceof List<?> list) {
                sb.append(indentStr).append(key).append(":\n");
                String listIndentStr = repeat("  ", indent + 1);

                for (Object item : list) {
                    sb.append(listIndentStr)
                            .append("- \"")
                            .append(escapeString(item.toString()))
                            .append("\"\n");
                }
            } else if (value instanceof MultiLineString mls) {
                sb.append(indentStr).append(key).append(": |\n");
                for (String line : mls.lines) {
                    sb.append(indentStr).append("  ").append(line).append("\n");
                }
            } else if (value instanceof String) {
                sb.append(indentStr).append(key).append(": \"").append(escapeString((String) value)).append("\"\n");
            } else if (value instanceof Double || value instanceof Float) {
                sb.append(indentStr).append(key).append(": ").append(value).append("\n");
            } else if (value instanceof Number) {
                sb.append(indentStr).append(key).append(": ").append(value).append("\n");
            } else if (value instanceof Boolean) {
                sb.append(indentStr).append(key).append(": ").append(value).append("\n");
            } else {
                sb.append(indentStr).append(key).append(": \"").append(escapeString(value.toString())).append("\"\n");
            }

            writtenPaths.add(fullPath);
            lastTopSection = currentTopSection;
        }
    }

    /**
     * Escapes special characters in a string for YAML.
     */
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Repeats a string n times.
     */
    private String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Gets the file associated with this builder.
     *
     * @return The configuration file
     */
    public File getFile() {
        return file;
    }

    /**
     * Inner class to represent multi-line strings.
     */
    private record MultiLineString(String[] lines) {
    }

    public static class YAMLSection {
        private final YAMLBuilder parent;
        private final String basePath;
        private final Map<String, Object> data;

        YAMLSection(YAMLBuilder parent, String basePath, Map<String, Object> data) {
            this.parent = parent;
            this.basePath = basePath;
            this.data = data;
        }

        /**
         * Gets the full path for a relative key.
         */
        private String getFullPath(String key) {
            return basePath.isEmpty() ? key : basePath + "." + key;
        }

        /**
         * Gets a String value from this section.
         */
        public String getString(String path) {
            return parent.getString(getFullPath(path));
        }

        /**
         * Gets a String value with default.
         */
        public String getString(String path, String defaultValue) {
            return parent.getString(getFullPath(path), defaultValue);
        }

        /**
         * Gets a boolean value from this section.
         */
        public boolean getBoolean(String path) {
            return parent.getBoolean(getFullPath(path));
        }

        /**
         * Gets a boolean value with default.
         */
        public boolean getBoolean(String path, boolean defaultValue) {
            return parent.getBoolean(getFullPath(path), defaultValue);
        }

        /**
         * Gets an int value from this section.
         */
        public int getInt(String path) {
            return parent.getInt(getFullPath(path));
        }

        /**
         * Gets an int value with default.
         */
        public int getInt(String path, int defaultValue) {
            return parent.getInt(getFullPath(path), defaultValue);
        }

        /**
         * Gets a long value from this section.
         */
        public long getLong(String path) {
            return parent.getLong(getFullPath(path));
        }

        /**
         * Gets a long value with default.
         */
        public long getLong(String path, long defaultValue) {
            return parent.getLong(getFullPath(path), defaultValue);
        }

        /**
         * Gets a double value from this section.
         */
        public double getDouble(String path) {
            return parent.getDouble(getFullPath(path));
        }

        /**
         * Gets a double value with default.
         */
        public double getDouble(String path, double defaultValue) {
            return parent.getDouble(getFullPath(path), defaultValue);
        }

        /**
         * Gets a float value from this section.
         */
        public float getFloat(String path) {
            return parent.getFloat(getFullPath(path));
        }

        /**
         * Gets a float value with default.
         */
        public float getFloat(String path, float defaultValue) {
            return parent.getFloat(getFullPath(path), defaultValue);
        }

        /**
         * Gets a List of Strings from this section.
         */
        public List<String> getStringList(String path) {
            return parent.getStringList(getFullPath(path));
        }

        /**
         * Gets a List of Strings with default.
         */
        public List<String> getStringList(String path, List<String> defaultValue) {
            return parent.getStringList(getFullPath(path), defaultValue);
        }

        /**
         * Gets a List of Integers from this section.
         */
        public List<Integer> getIntList(String path) {
            return parent.getIntList(getFullPath(path));
        }

        /**
         * Gets a List of Doubles from this section.
         */
        public List<Double> getDoubleList(String path) {
            return parent.getDoubleList(getFullPath(path));
        }

        /**
         * Gets a raw Object value from this section.
         */
        public Object get(String path) {
            return parent.get(getFullPath(path));
        }

        /**
         * Gets a raw Object value with default.
         */
        public Object get(String path, Object defaultValue) {
            return parent.get(getFullPath(path), defaultValue);
        }

        /**
         * Checks if a path exists in this section.
         */
        public boolean contains(String path) {
            return parent.contains(getFullPath(path));
        }

        /**
         * Gets a nested configuration section.
         */
        @SuppressWarnings("unchecked")
        public YAMLSection getConfigurationSection(String path) {
            Object value = data.get(path);
            if (value instanceof Map) {
                return new YAMLSection(parent, getFullPath(path), (Map<String, Object>) value);
            }
            return parent.getConfigurationSection(getFullPath(path));
        }

        /**
         * Gets the keys in this section.
         *
         * @param deep If true, returns all keys recursively
         * @return Set of keys
         */
        public Set<String> getKeys(boolean deep) {
            Set<String> keys = new LinkedHashSet<>();
            if (deep) {
                parent.collectKeysDeep(data, "", keys);
            } else {
                keys.addAll(data.keySet());
            }
            return keys;
        }

        /**
         * Gets the base path of this section.
         */
        public String getCurrentPath() {
            return basePath;
        }

        /**
         * Gets the name of this section (last part of path).
         */
        public String getName() {
            int lastDot = basePath.lastIndexOf('.');
            return lastDot >= 0 ? basePath.substring(lastDot + 1) : basePath;
        }

        /**
         * Gets the parent YAMLBuilder.
         */
        public YAMLBuilder getRoot() {
            return parent;
        }
    }
}