package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static me.yleoft.zAPI.utils.ItemStackUtils.legacyColors;

/**
 * Utility class for Material related operations.
 */
public abstract class MaterialUtils {

    /**
     * Gets a Material from a string.
     * @param materialString The string to get the material from.
     * @return The Material.
     */
    @NotNull
    public static Material getMaterial(@NotNull final String materialString) {
        if(StringUtils.startsWithIgnoreCase(materialString, "[head]")) {
            return Material.PLAYER_HEAD;
        }
        if(isLegacyMaterial(materialString)) {
            if (legacyColors.keySet().stream().anyMatch(materialString::startsWith)) {
                return getLegacyMaterial(legacyColors.keySet().stream()
                        .filter(materialString::startsWith)
                        .findFirst()
                        .map(prefix -> materialString.substring(prefix.length()+1))
                        .orElse(materialString));
            }
        }
        return Objects.requireNonNull(Material.getMaterial(materialString));
    }

    /**
     * Gets a Legacy Material from a string.
     * @param name The string to get the material from.
     * @return The Legacy Material.
     */
    @NotNull
    public static Material getLegacyMaterial(@NotNull final String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Material.STONE; // Fallback
        }
    }

    /**
     * Checks if a material is a legacy material.
     * @param name The string to check.
     * @return True if the material is a legacy material, false otherwise.
     */
    public static boolean isLegacyMaterial(@NotNull final String name) {
        try {
            Material.valueOf(name);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Finds a Material from a list of names.
     * @param names The names to find the material from.
     * @return The Material.
     */
    @NotNull
    public static Material findMaterial(@NotNull final String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.COBBLESTONE;
    }

}
