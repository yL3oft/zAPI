package me.yleoft.zAPI.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.yleoft.zAPI.zAPI;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for string operations, including color and placeholder handling.
 * This class provides methods to transform strings by applying color codes, hex codes, and placeholders.
 */
public abstract class StringUtils {

    /**
     * Check if a string starts with a given prefix, ignoring case.
     * @param full The full string to check
     * @param prefix The prefix to check against
     * @return true if the full string starts with the prefix, false otherwise
     */
    public static boolean startsWithIgnoreCase(@NotNull final String full, @NotNull final String prefix) {
        if (prefix.length() > full.length()) return false;
        return full.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    /**
     * Transform a string by applying color codes, hex codes and placeholders.
     * @param p The player to apply placeholders for
     * @param string The string to transform
     * @return The transformed string
     */
    @NotNull
    public static String transform(@Nullable final OfflinePlayer p, @NotNull String string) {
        string = hex(string);
        string = color(string);
        string = applyPlaceholders(p, string);
        return string;
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param string The string to transform
     * @return The transformed string
     */
    @NotNull
    public static String transform(@NotNull String string) {
        return transform(null, string);
    }

    /**
     * Apply placeholders to a string for a specific player.
     * @param p The player to apply placeholders for
     * @param string The string to apply placeholders to
     * @return The string with placeholders applied
     */
    @NotNull
    public static String applyPlaceholders(@Nullable final OfflinePlayer p, @NotNull String string) {
        if (p != null && zAPI.getPlugin().getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
            string = PlaceholderAPI.setPlaceholders(p, string);
        return applyOwnPlaceholders(p, string);
    }

    /**
     * Apply placeholders to a string for a specific player using your own placeholder expansion.
     * @param p The player to apply placeholders for
     * @param string The string to apply placeholders to
     * @return The string with placeholders applied
     */
    @NotNull
    public static String applyOwnPlaceholders(@Nullable final OfflinePlayer p, @NotNull String string) {
       return zAPI.getPlaceholderAPIHandler().applyPlaceholders(p, string);
    }

    /**
     * Apply color codes to a string.
     * @param string The string to apply color codes to
     * @return The string with color codes applied
     */
    @NotNull
    public static String color(@NotNull String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Apply hex color codes to a string.
     * @param string The string to apply hex color codes to
     * @return The string with hex color codes applied
     */
    @NotNull
    public static String hex(@NotNull String string) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(string);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append("&").append(hex.charAt(i));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Check if a string is an integer.
     * @param strNum The string to check
     * @return true if the string is an integer, false otherwise
     */
    public static boolean isInteger(@NotNull String strNum) {
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Parse a string as a time in milliseconds.
     * The string can be in the format "1h", "30m", "15s", etc.
     * @param time The time string to parse
     * @return The time in milliseconds, or 0 if the string is empty
     */
    public static long parseAsTime(@NotNull String time) {
        return TimeParser.parseTimeToSeconds(time);
    }

    /**
     * Parse milliseconds to time string.
     * @param time The time in milliseconds to parse
     * @return The time parsed as time string
     */
    public static String parseAsString(long time) {
        return TimeParser.formatMsToString(time);
    }

    private static class TimeParser {

        private static final long MS_IN_YEAR = 365L * 24 * 60 * 60 * 1000;
        private static final long MS_IN_MONTH = 30L * 24 * 60 * 60 * 1000;
        private static final long MS_IN_WEEK = 7L * 24 * 60 * 60 * 1000;
        private static final long MS_IN_DAY = 24L * 60 * 60 * 1000;
        private static final long MS_IN_HOUR = 60L * 60 * 1000;
        private static final long MS_IN_MINUTE = 60L * 1000;

        private static final Pattern TIME_PATTERN = Pattern.compile(
                "(\\d+)(y|mo|w|d|h|m|s)", Pattern.CASE_INSENSITIVE
        );

        public static long parseTimeToSeconds(String timeString) {
            if (timeString == null || timeString.isEmpty()) {
                throw new IllegalArgumentException("Time string cannot be null or empty");
            }

            Matcher matcher = TIME_PATTERN.matcher(timeString);
            long totalSeconds = 0;

            while (matcher.find()) {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();
                switch (unit) {
                    case "y":
                        totalSeconds += value * MS_IN_YEAR;
                        break;
                    case "mo":
                        totalSeconds += value * MS_IN_MONTH;
                        break;
                    case "w":
                        totalSeconds += value * MS_IN_WEEK;
                        break;
                    case "d":
                        totalSeconds += value * MS_IN_DAY;
                        break;
                    case "h":
                        totalSeconds += value * MS_IN_HOUR;
                        break;
                    case "m":
                        totalSeconds += value * MS_IN_MINUTE;
                        break;
                    case "s":
                        totalSeconds += value;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown time unit: " + unit);
                }
            }

            return totalSeconds;
        }

        public static String formatMsToString(long totalSeconds) {
            if (totalSeconds <= 0) {
                return "0s";
            }

            StringBuilder sb = new StringBuilder();

            long years = totalSeconds / MS_IN_YEAR;
            if (years > 0) {
                sb.append(years).append("y");
                totalSeconds %= MS_IN_YEAR;
            }

            long months = totalSeconds / MS_IN_MONTH;
            if (months > 0) {
                sb.append(months).append("mo");
                totalSeconds %= MS_IN_MONTH;
            }

            long weeks = totalSeconds / MS_IN_WEEK;
            if (weeks > 0) {
                sb.append(weeks).append("w");
                totalSeconds %= MS_IN_WEEK;
            }

            long days = totalSeconds / MS_IN_DAY;
            if (days > 0) {
                sb.append(days).append("d");
                totalSeconds %= MS_IN_DAY;
            }

            long hours = totalSeconds / MS_IN_HOUR;
            if (hours > 0) {
                sb.append(hours).append("h");
                totalSeconds %= MS_IN_HOUR;
            }

            long minutes = totalSeconds / MS_IN_MINUTE;
            if (minutes > 0) {
                sb.append(minutes).append("m");
                totalSeconds %= MS_IN_MINUTE;
            }

            if (totalSeconds > 0) {
                sb.append(totalSeconds).append("s");
            }

            return sb.toString();
        }
    }

}
