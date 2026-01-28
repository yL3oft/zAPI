package me.yleoft.zAPI.handlers;

import me.yleoft.zAPI.utility.Version;
import me.yleoft.zAPI.zAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base handler for plugin-defined placeholders.
 *
 * <p>Implementations should return a string replacement for the given params, or null if the placeholder
 * is unknown.</p>
 */
public interface PlaceholdersHandler {

    /**
     * Gets the identifier prefix for this handler.
     *
     * @return The identifier prefix
     */
    @NotNull
    String getIdentifier();

    /**
     * Gets the name of the plugin this handler is for.
     *
     * @return The plugin name
     */
    @NotNull
    default String getAuthor() {
        return String.join(", ", Version.getAuthors());
    };

    /**
     * Gets the version of the plugin this handler is for.
     *
     * @return The plugin version
     */
    @NotNull
    default String getVersion() {
        return Version.getVersion();
    };

    /**
     * Applies plugin placeholders for a specific player.
     *
     * @param player The player to apply placeholders for
     * @param params The placeholder params (the part after the identifier prefix)
     * @return The replacement, or null if not handled
     */
    @Nullable
    String applyHookPlaceholders(@Nullable OfflinePlayer player, @NotNull String params);

    /**
     * Applies plugin placeholders in a given text for a specific player.
     *
     * @param player The player to apply placeholders for
     * @param text   The text containing placeholders
     * @return The text with placeholders replaced
     */
    default String applyPlaceholders(@Nullable OfflinePlayer player, @NotNull String text) {
        if (!text.contains("%")) return text;

        Pattern pattern = Pattern.compile("%"+ getIdentifier()+"_([^%]+)%");
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