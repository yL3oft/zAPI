package me.yleoft.zAPI.utility;

import me.clip.placeholderapi.PlaceholderAPI;
import me.yleoft.zAPI.zAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TextFormatter {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)(y|mo|w|d|h|m|s)", Pattern.CASE_INSENSITIVE);
    private static final boolean PAPI_AVAILABLE;

    private static final Map<String, Long> TIME_UNITS = Map.of(
            "y", 365L * 24 * 60 * 60 * 1000,
            "mo", 30L * 24 * 60 * 60 * 1000,
            "w", 7L * 24 * 60 * 60 * 1000,
            "d", 24L * 60 * 60 * 1000,
            "h", 60L * 60 * 1000,
            "m", 60L * 1000,
            "s", 1000L
    );
    private static final String[] TIME_SUFFIXES = {"y", "mo", "w", "d", "h", "m", "s"};

    // For unit tests
    static {
        boolean available;
        try {
            available = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (NoClassDefFoundError | NullPointerException e) {
            available = false;
        }
        PAPI_AVAILABLE = available;
    }

    /**
     * Check if a string starts with a given prefix, ignoring case.
     * @param full The full string to check
     * @param prefix The prefix to check against
     * @return true if the full string starts with the prefix, false otherwise
     */
    public static boolean startsWithIgnoreCase(@NotNull String full, @NotNull String prefix) {
        return full.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Transform a string by applying color codes, hex codes and placeholders and turns it into a Component.
     * @param p The player to apply placeholders for
     * @param string The string to transform
     * @return The transformed component
     */
    @NotNull
    public static Component transform(@Nullable OfflinePlayer p, @NotNull String string) {
        return zAPI.getMiniMessage().deserialize(transformString(p, string));
    }

    /**
     * Transform a string by applying color codes, hex codes and placeholders.
     * @param p The player to apply placeholders for
     * @param string The string to transform
     * @return The transformed string
     */
    @NotNull
    public static String transformString(@Nullable OfflinePlayer p, @NotNull String string) {
        return applyPlaceholders(p, string);
    }

    /**
     * Transform a string by applying color codes, hex codes and placeholders.
     * @param p The player to apply placeholders for
     * @param component The component to transform
     * @return The transformed component
     */
    @NotNull
    public static Component transform(@Nullable OfflinePlayer p, @NotNull Component component) {
        return zAPI.getMiniMessage().deserialize(transformString(p, component));
    }

    /**
     * Transform a string by applying color codes, hex codes and placeholders.
     * @param p The player to apply placeholders for
     * @param component The component to transform
     * @return The transformed string
     */
    @NotNull
    public static String transformString(@Nullable OfflinePlayer p, @NotNull Component component) {
        return applyPlaceholders(p, zAPI.getMiniMessage().serialize(component));
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param string The string to transform
     * @return The transformed string
     */
    @NotNull
    public static Component transform(@NotNull String string) {
        return transform(null, string);
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param string The string to transform
     * @return The transformed string
     */
    @NotNull
    public static String transformString(@NotNull String string) {
        return transformString(null, string);
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param component The component to transform
     * @return The transformed string
     */
    @NotNull
    public static Component transform(@NotNull Component component) {
        return transform(null, component);
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param component The component to transform
     * @return The transformed string
     */
    @NotNull
    public static String transformString(@NotNull Component component) {
        return transformString(null, component);
    }

    /**
     * Apply placeholders to a string for a specific player.
     * @param p The player to apply placeholders for
     * @param string The string to apply placeholders to
     * @return The string with placeholders applied
     */
    @NotNull
    public static String applyPlaceholders(@Nullable final OfflinePlayer p, @NotNull String string) {
        if (p != null && PAPI_AVAILABLE)
            string = PlaceholderAPI.setPlaceholders(p, string);
        return string;
    }

    /**
     * Check if a string is an integer.
     * @param strNum The string to check
     * @return true if the string is an integer, false otherwise
     */
    public static boolean isInteger(@NotNull String strNum) {
        try {
            Integer.parseInt(strNum);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Parse a string as a time in milliseconds.
     * The string can be in the format "1h", "30m", "15s", etc.
     * @param time The time string to parse
     * @return The time in milliseconds, or 0 if the string is empty
     */
    public static long parseAsTime(String time) {
        if (time == null || time.isEmpty()) return 0L;
        Matcher matcher = TIME_PATTERN.matcher(time);
        long totalMs = 0;
        while (matcher.find()) {
            totalMs += Long.parseLong(matcher.group(1)) * TIME_UNITS.get(matcher.group(2).toLowerCase());
        }
        return totalMs;
    }

    /**
     * Parse milliseconds to time string.
     * @param ms The time in milliseconds to parse
     * @return The time parsed as time string
     */
    public static String parseAsString(long ms) {
        if (ms <= 0) return "0s";
        StringBuilder sb = new StringBuilder();
        for (String unit : TIME_SUFFIXES) {
            long unitMs = TIME_UNITS.get(unit);
            if (ms >= unitMs) {
                sb.append(ms / unitMs).append(unit);
                ms %= unitMs;
            }
        }
        return sb.toString();
    }

}
