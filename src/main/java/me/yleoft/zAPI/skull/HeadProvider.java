package me.yleoft.zAPI.skull;

import me.yleoft.zAPI.zAPI;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for handling player heads.
 * Provides methods to retrieve player heads based on player names, base64 or custom strings.
 */
public abstract class HeadProvider {

    private static final Map<String, ItemStack> cacheName = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack> cacheBase64 = new ConcurrentHashMap<>();

    /**
     * Retrieves a player head based on the provided type and value.
     * Supported types:
     * - "head" for player names.
     * @param type  The type of head to retrieve.
     * @param value The value associated with the type (e.g., player name).
     * @return An ItemStack representing the player head, or a default head if the type is unsupported or failed to load.
     */
    public static ItemStack getPlayerHeadFromString(@NotNull final String type, @NotNull final String value) {
        switch (type) {
            case "head": {
                if(value.startsWith("http://") || value.startsWith("https://")) {
                    return getPlayerHeadFromString("urlhead", value);
                } else if(value.length() >= 32) {
                    return getPlayerHeadFromString("base64head", value);
                } else {
                    return getPlayerHeadFromString("namehead", value);
                }
            }
            case "namehead": {
                return getPlayerHead(value);
            }
            case "base64head": {
                return getPlayerHeadB64(value);
            }
            case "urlhead": {
                String encoded = getEncoded(value);
                return getPlayerHeadB64(encoded);
            }
            default: return originalHead.clone();
        }
    }

    /**
     * Retrieves a player head based on the player's name.
     * Caches the result to improve performance on subsequent calls.
     *
     * @param playerName The name of the player whose head is to be retrieved.
     * @return An ItemStack representing the player's head, or a default head if retrieval fails.
     */
    public static ItemStack getPlayerHead(@NotNull final String playerName) {
        try {
            return cacheName.computeIfAbsent(playerName, SkullUtils::getSkullByName).clone();
        } catch (Exception exception) {
            zAPI.getPlugin().getLogger().severe("[zAPI] Failed to get head for player: " + playerName);
        }
        return originalHead.clone();
    }

    /**
     * Retrieves a player head based on a base64 encoded texture URL.
     * Caches the result to improve performance on subsequent calls.
     *
     * @param base64Url The base64 encoded texture URL of the player's head.
     * @return An ItemStack representing the player's head, or a default head if retrieval fails.
     */
    public static ItemStack getPlayerHeadB64(@NotNull final String base64Url) {
        try {
            return cacheBase64.computeIfAbsent(base64Url, SkullUtils::getSkullByBase64EncodedTextureUrl).clone();
        } catch (Exception exception) {
            zAPI.getPlugin().getLogger().severe("[zAPI] Failed to get head for base64: " + base64Url);
        }
        return originalHead.clone();
    }

}
