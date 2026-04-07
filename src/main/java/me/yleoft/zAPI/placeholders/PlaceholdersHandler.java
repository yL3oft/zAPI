package me.yleoft.zAPI.placeholders;

import me.yleoft.zAPI.hook.HookRegistry;
import me.yleoft.zAPI.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base handler for plugin-defined placeholders.
 *
 * <p>Implementations should define their placeholders via {@link #getPlaceholders()} and
 * implement {@link #onPlaceholderRequest(OfflinePlayer, String, List)} for MiniPlaceholders,
 * and override {@link #applyHookPlaceholders(OfflinePlayer, String)} for PAPI's raw param parsing
 * when the placeholder structure is too complex for simple key+args routing.</p>
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
     * Gets the author of the plugin this handler is for.
     *
     * @return The plugin author(s)
     */
    @NotNull
    default String getAuthor() {
        return String.join(", ", Version.getAuthors());
    }

    /**
     * Gets the version of the plugin this handler is for.
     *
     * @return The plugin version
     */
    @NotNull
    default String getVersion() {
        return Version.getVersion();
    }

    /**
     * Returns the list of placeholder definitions this handler provides.
     *
     * <p>These definitions are used to register placeholders with MiniPlaceholders.
     * Each definition declares a key, how many colon-separated arguments it expects,
     * and a {@link PlaceholderType} that controls how it is registered.</p>
     *
     * @return A list of placeholder definitions
     */
    @NotNull
    List<PlaceholderDefinition> getPlaceholders();

    /**
     * Called when a placeholder value is requested via MiniPlaceholders.
     * Also used as the common entry point when PAPI params can be cleanly routed.
     *
     * @param player     The player to resolve the placeholder for (may be null for GLOBAL placeholders)
     * @param key        The placeholder key (e.g., "home_world", "set")
     * @param parameters The parameter values in order (e.g., ["1"] or ["1", "2"])
     * @return The replacement string, or null if not handled
     */
    @Nullable
    String onPlaceholderRequest(@Nullable OfflinePlayer player, @NotNull String key, @NotNull List<String> parameters);

    /**
     * Handles PlaceholderAPI's raw params string.
     *
     * <p>Override this for complex PAPI parsing (e.g., player prefixes, optional params,
     * variable-depth structures). The default implementation tries to match against
     * registered {@link PlaceholderDefinition}s.</p>
     *
     * <p>Supports {@code player_<name>_<key>_<args>} syntax for
     * {@link PlaceholderType#PLAYER_TARGETED} placeholders. For example:
     * {@code %zhomes_player_Steve_home_1%} resolves "home" for player "Steve" with arg "1".</p>
     *
     * @param player The player to apply placeholders for
     * @param params The raw placeholder params (the part after the identifier prefix)
     * @return The replacement, or null if not handled
     */
    @Nullable
    default String applyHookPlaceholders(@Nullable OfflinePlayer player, @NotNull String params) {
        // Check for player_<name>_<key> prefix (PAPI player-targeted syntax)
        if (params.startsWith("player_")) {
            String afterPlayer = params.substring("player_".length()); // e.g. "Steve_home_1"

            // Try to match each PLAYER_TARGETED definition against the remainder
            for (PlaceholderDefinition def : getPlaceholders()) {
                if (def.getType() != PlaceholderType.PLAYER_TARGETED) continue;

                String keySuffix = "_" + def.getKey(); // e.g. "_home"

                // Find where the key starts — everything between "player_" and "_<key>" is the player name
                int keyIndex = afterPlayer.indexOf(keySuffix);
                if (keyIndex <= 0) continue;

                String targetName = afterPlayer.substring(0, keyIndex);
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

                if (!def.hasParameters()) {
                    // Exact match: player_Steve_home
                    if (afterPlayer.equals(targetName + keySuffix)) {
                        return onPlaceholderRequest(targetPlayer, def.getKey(), Collections.emptyList());
                    }
                } else {
                    // With args: player_Steve_home_1 or player_Steve_home_x_1_2
                    String fullPrefix = targetName + keySuffix + "_";
                    if (afterPlayer.startsWith(fullPrefix)) {
                        String remaining = afterPlayer.substring(fullPrefix.length());
                        String[] parts = remaining.split("_", def.getParameterCount());
                        return onPlaceholderRequest(targetPlayer, def.getKey(), List.of(parts));
                    }
                    // Exact match with key only (no trailing args but definition expects them)
                    if (afterPlayer.equals(targetName + keySuffix)) {
                        return onPlaceholderRequest(targetPlayer, def.getKey(), Collections.emptyList());
                    }
                }
            }
        }

        // Default: try exact key match from definitions (AUDIENCE and GLOBAL)
        for (PlaceholderDefinition def : getPlaceholders()) {
            if (!def.hasParameters() && params.equals(def.getKey())) {
                return onPlaceholderRequest(player, def.getKey(), Collections.emptyList());
            }
            if (def.hasParameters() && params.startsWith(def.getKey() + "_")) {
                String remaining = params.substring(def.getKey().length() + 1);
                String[] parts = remaining.split("_", def.getParameterCount());
                return onPlaceholderRequest(player, def.getKey(), List.of(parts));
            }
        }
        return onPlaceholderRequest(player, params, Collections.emptyList());
    }

    /**
     * Applies plugin placeholders in a given text for a specific player.
     * Supports both PAPI-style {@code %identifier_...%} and MiniPlaceholders-style
     * {@code <identifier_...:args>} placeholders.
     *
     * @param player The player to apply placeholders for
     * @param text   The text containing placeholders
     * @return The text with placeholders replaced
     */
    default String applyPlaceholders(@Nullable OfflinePlayer player, @NotNull String text) {
        if (text == null || text.isEmpty()) return text;

        // Handle %identifier_...% placeholders
        if (text.contains("%")) {
            Pattern pattern = Pattern.compile("%" + Pattern.quote(getIdentifier()) + "_([^%]+)%");
            Matcher matcher = pattern.matcher(text);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String rawParams = matcher.group(1);
                String replacement = applyHookPlaceholders(player, rawParams);
                if (replacement == null) replacement = "";
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            text = buffer.toString();
        }

        // Handle <identifier_...> when MiniPlaceholders is NOT present (fallback)
        if (!HookRegistry.MINI_PLACEHOLDERS.exists() && text.contains("<")) {
            Pattern miniPattern = Pattern.compile("<" + Pattern.quote(getIdentifier()) + "_([^>]+)>");
            Matcher miniMatcher = miniPattern.matcher(text);
            StringBuffer miniBuffer = new StringBuffer();

            while (miniMatcher.find()) {
                String rawTag = miniMatcher.group(1);
                String[] tagParts = rawTag.split(":");
                String miniKey = tagParts[0];
                List<String> args = tagParts.length > 1
                        ? List.of(Arrays.copyOfRange(tagParts, 1, tagParts.length))
                        : Collections.emptyList();

                String replacement = onPlaceholderRequest(player, miniKey, args);
                if (replacement == null) replacement = "";
                miniMatcher.appendReplacement(miniBuffer, Matcher.quoteReplacement(replacement));
            }
            miniMatcher.appendTail(miniBuffer);
            text = miniBuffer.toString();
        }

        return text;
    }
}