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
        if (full == null || prefix == null) return false;
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

}
