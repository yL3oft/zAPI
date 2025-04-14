package me.yleoft.zAPI.handlers;

import me.yleoft.zAPI.zAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPIHandler {

    private final zAPI zAPI;

    /**
     * PlaceholderAPIHandler constructor
     * @param zAPI zAPI instance
     */
    public PlaceholderAPIHandler(zAPI zAPI) {
        this.zAPI = zAPI;
    }

    /**
     * Parses the placeholders for a specific player.
     * @param player The player to apply placeholders for
     * @param params The string to apply placeholders to
     * @return The parsed placeholder
     */
    public String applyHookPlaceholders(@Nullable Player player, @NotNull String params) {
        return "";
    }

    /**
     * Apply placeholders to a string for a specific player.
     * @param player The player to apply placeholders for
     * @param text The string to apply placeholders to
     * @return The string with placeholders applied
     */
    public String applyPlaceholders(@Nullable Player player, @NotNull String text) {
        if (text == null || !text.contains("%")) return text;

        Pattern pattern = Pattern.compile("%"+zAPI.getPlugin().getDescription().getName().toLowerCase()+"_([^%]+)%");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String identifier = matcher.group(1);
            String replacement = applyHookPlaceholders(player, identifier);
            if (replacement == null) replacement = "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
