package me.yleoft.zAPI.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.yleoft.zAPI.zAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
     * Transform a string by applying color codes, hex codes, placeholders and converting links.
     * @param p The player to apply placeholders for
     * @param string The string to transform
     * @return The transformed TextComponent array
     */
    @NotNull
    public static TextComponent[] transformTC(@Nullable final OfflinePlayer p, @NotNull String string) {
        string = hex(string);
        string = color(string);
        string = applyPlaceholders(p, string);
        return link(string);
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
     * Transform a string by applying color codes, hex codes and converting links.
     * @param string The string to transform
     * @return The transformed TextComponent array
     */
    @NotNull
    public static TextComponent[] transformTC(@NotNull String string) {
        return transformTC(null, string);
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
     * Convert URLs in a string to clickable links (With text if needed).
     * @param string The string to convert
     * @return The string with clickable links
     */
    public static TextComponent[] link(String string) {
        Pattern pattern = Pattern.compile("\\[([^]]+)]\\((cmd_[^)]+)\\)|\\[([^]]+)]\\((https?://[^)\\s]+)\\)");
        Matcher matcher = pattern.matcher(string);
        TextComponent message = new TextComponent();
        int lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String before = string.substring(lastIndex, matcher.start());
                message.addExtra(new TextComponent(before));
            }
            if (matcher.group(2) != null) {
                String text = matcher.group(1);
                String cmd = matcher.group(2).substring(4);
                TextComponent link = new TextComponent(text);
                link.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                message.addExtra(link);
            } else {
                String text = matcher.group(3);
                String url = matcher.group(4);
                TextComponent link = new TextComponent(text);
                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                message.addExtra(link);
            }
            lastIndex = matcher.end();
        }
        if (lastIndex < string.length()) {
            message.addExtra(new TextComponent(string.substring(lastIndex)));
        }
        return new TextComponent[]{message};
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
        return TimeParser.parseTimeToMilliseconds(time);
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

        // Regex pattern to match time strings like "1y", "2mo", "3w", "4d", "5h", "6m", "7s"
        private static final Pattern TIME_PATTERN = Pattern.compile(
                "(\\d+)(y|mo|w|d|h|m|s)", Pattern.CASE_INSENSITIVE
        );

        /**
         * Parse a time string to milliseconds.
         * @param timeString The time string to parse
         * @return The time in milliseconds
         */
        public static long parseTimeToMilliseconds(String timeString) {
            if (timeString == null || timeString.isEmpty()) {
                throw new IllegalArgumentException("Time string cannot be null or empty");
            }

            Matcher matcher = TIME_PATTERN.matcher(timeString);
            long totalMs = 0;

            while (matcher.find()) {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();
                switch (unit) {
                    case "y":
                        totalMs += value * MS_IN_YEAR;
                        break;
                    case "mo":
                        totalMs += value * MS_IN_MONTH;
                        break;
                    case "w":
                        totalMs += value * MS_IN_WEEK;
                        break;
                    case "d":
                        totalMs += value * MS_IN_DAY;
                        break;
                    case "h":
                        totalMs += value * MS_IN_HOUR;
                        break;
                    case "m":
                        totalMs += value * MS_IN_MINUTE;
                        break;
                    case "s":
                        // seconds -> convert to milliseconds for consistency
                        totalMs += value * 1000L;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown time unit: " + unit);
                }
            }

            return totalMs;
        }

        /**
         * Format milliseconds to a time string.
         * @param totalMs The total milliseconds to format
         * @return The formatted time string
         */
        public static String formatMsToString(long totalMs) {
            if (totalMs <= 0) {
                return "0s";
            }

            StringBuilder sb = new StringBuilder();

            long years = totalMs / MS_IN_YEAR;
            if (years > 0) {
                sb.append(years).append("y");
                totalMs %= MS_IN_YEAR;
            }

            long months = totalMs / MS_IN_MONTH;
            if (months > 0) {
                sb.append(months).append("mo");
                totalMs %= MS_IN_MONTH;
            }

            long weeks = totalMs / MS_IN_WEEK;
            if (weeks > 0) {
                sb.append(weeks).append("w");
                totalMs %= MS_IN_WEEK;
            }

            long days = totalMs / MS_IN_DAY;
            if (days > 0) {
                sb.append(days).append("d");
                totalMs %= MS_IN_DAY;
            }

            long hours = totalMs / MS_IN_HOUR;
            if (hours > 0) {
                sb.append(hours).append("h");
                totalMs %= MS_IN_HOUR;
            }

            long minutes = totalMs / MS_IN_MINUTE;
            if (minutes > 0) {
                sb.append(minutes).append("m");
                totalMs %= MS_IN_MINUTE;
            }

            long seconds = totalMs / 1000L;
            if (seconds > 0) {
                sb.append(seconds).append("s");
            }

            return sb.toString();
        }
    }

}
