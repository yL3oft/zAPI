package me.yleoft.zAPI.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.yleoft.zAPI.zAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    private zAPI zAPI;

    /**
     * StringUtils constructor
     * @param zAPI zAPI instance
     */
    public StringUtils(@NotNull zAPI zAPI) {
        this.zAPI = zAPI;

    }

    public static boolean startsWithIgnoreCase(String full, String prefix) {
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
    public String transform(@Nullable Player p, @NotNull String string) {
        string = hex(string);
        string = color(string);
        if(p != null) {
            string = applyPlaceholders(p, string);
        }
        return string;
    }

    /**
     * Transform a string by applying color codes and hex codes.
     * @param string The string to transform
     * @return The transformed string
     */
    public String transform(@NotNull String string) {
        return transform(null, string);
    }

    /**
     * Apply placeholders to a string for a specific player.
     * @param p The player to apply placeholders for
     * @param string The string to apply placeholders to
     * @return The string with placeholders applied
     */
    public String applyPlaceholders(@Nullable Player p, @NotNull String string) {
        if (zAPI.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
            string = PlaceholderAPI.setPlaceholders(p, string);
        return applyOwnPlaceholders(p, string);
    }

    /**
     * Apply placeholders to a string for a specific player using your own placeholder expansion.
     * @param p The player to apply placeholders for
     * @param string The string to apply placeholders to
     * @return The string with placeholders applied
     */
    public String applyOwnPlaceholders(@Nullable Player p, @NotNull String string) {
       return zAPI.getPlaceholderAPIHandler().applyPlaceholders(p, string);
    }

    /**
     * Apply color codes to a string.
     * @param string The string to apply color codes to
     * @return The string with color codes applied
     */
    public String color(@NotNull String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Apply hex color codes to a string.
     * @param string The string to apply hex color codes to
     * @return The string with hex color codes applied
     */
    public String hex(@NotNull String string) {
        Matcher matcher = Pattern.compile("&#([A-Za-z0-9]{6})").matcher(string);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = "&x" + matcher.group(1).replaceAll("(.)", "&$1");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        return matcher.appendTail(result).toString();
    }

}
