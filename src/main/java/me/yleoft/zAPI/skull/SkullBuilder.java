package me.yleoft.zAPI.skull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.yleoft.zAPI.utility.Version;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SkullUtils is a utility class for handling player skulls in Minecraft.
 * It provides methods to get the correct head material based on the server version,
 * and to create skull ItemStacks from player names.
 */
public abstract class SkullBuilder {

    private static final Gson GSON = new Gson();
    protected static final ItemStack originalHead;

    static {
        // Initialize the original head ItemStack based on the server version
        originalHead = getHeadItemStack();
    }

    /**
     * Get the material for player heads based on the server version
     *
     * @return Material for player heads
     */
    public static ItemStack getHeadItemStack() {
        return isNewHead() ? new ItemStack(Material.PLAYER_HEAD) : new ItemStack(Material.getMaterial("SKULL_ITEM"), 1, (short) 3);
    }

    /**
     * Check if the server is using the new head material
     *
     * @return true if the server is using the new head material, false otherwise
     */
    public static boolean isNewHead() {
        return Arrays.stream(Material.values()).map(Material::name).collect(Collectors.toList()).contains("PLAYER_HEAD");
    }

    /**
     * Get the base64 encoded texture URL for a given texture URL
     *
     * @param url the texture URL to encode
     * @return base64 encoded texture URL
     */
    @NotNull
    public static String getEncoded(@NotNull final String url) {
        final byte[] encodedData = Base64.getEncoder().encode(String
                .format("{textures:{SKIN:{url:\"%s\"}}}", "https://textures.minecraft.net/texture/" + url)
                .getBytes());
        return new String(encodedData);
    }

    /**
     * Get the skull from a player name
     *
     * @param name the player name to use
     * @return skull
     */
    @NotNull
    public static ItemStack getSkullByName(@NotNull final String name) {
        final ItemStack head = originalHead.clone();
        if (name.isEmpty()) {
            return head;
        }
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);

        if (Version.HAS_PLAYER_PROFILES && offlinePlayer.getPlayerProfile().getTextures().isEmpty()) {
            meta.setOwnerProfile(offlinePlayer.getPlayerProfile().update().join());
        } else {
            meta.setOwner(offlinePlayer.getName());
        }

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Get the skull from a base64 encoded texture URL
     *
     * @param base64Url the base64 encoded texture URL to use
     * @return skull
     */
    @NotNull
    public static ItemStack getSkullByBase64EncodedTextureUrl(@NotNull final String base64Url) {
        final ItemStack head = originalHead.clone();
        if (base64Url.isEmpty()) {
            return head;
        }

        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        if (HAS_PLAYER_PROFILES) {
            final PlayerProfile profile = getPlayerProfile(base64Url);
            headMeta.setOwnerProfile(profile);
            head.setItemMeta(headMeta);
            return head;
        }

        final GameProfile profile = getGameProfile(base64Url);
        final Field profileField;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
            zAPI.getPlugin().getLogger().severe("[zAPI] Failed to set profile for head: " + exception.getMessage());
        }
        head.setItemMeta(headMeta);
        return head;
    }

    /**
     * Get a GameProfile from a base64 encoded texture URL.
     * @param base64Url the base64 encoded texture URL to use
     * @return GameProfile with the texture property set
     */
    @NotNull
    private static GameProfile getGameProfile(@NotNull final String base64Url) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", base64Url));
        return profile;
    }

    /**
     * Get a PlayerProfile from a base64 encoded texture URL.
     * @param base64Url the base64 encoded texture URL to use
     * @return PlayerProfile with the skin texture set
     */
    @NotNull
    private static PlayerProfile getPlayerProfile(@NotNull final String base64Url) {
        final PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());

        final String decodedBase64 = decodeSkinUrl(base64Url);
        if (decodedBase64 == null) {
            return profile;
        }

        final PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL(decodedBase64));
        } catch (final MalformedURLException exception) {
            zAPI.getPlugin().getLogger().severe("[zAPI] Failed to set skin URL for player profile: " + exception.getMessage());
        }

        profile.setTextures(textures);
        return profile;
    }

    /**
     * Decode a base64 encoded texture string to extract the skin URL.
     * @param base64Texture the base64 encoded texture string
     * @return the skin URL if available, null otherwise
     */
    @Nullable
    public static String decodeSkinUrl(@NotNull final String base64Texture) {
        if(base64Texture.isEmpty()) return null;
        final String decoded = new String(Base64.getDecoder().decode(base64Texture));
        final JsonObject object = GSON.fromJson(decoded, JsonObject.class);

        final JsonElement textures = object.get("textures");

        if (textures == null) {
            return null;
        }

        final JsonElement skin = textures.getAsJsonObject().get("SKIN");

        if (skin == null) {
            return null;
        }

        final JsonElement url = skin.getAsJsonObject().get("url");
        return url == null ? null : url.getAsString();
    }

}
