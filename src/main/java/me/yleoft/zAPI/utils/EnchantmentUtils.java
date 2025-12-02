package me.yleoft.zAPI.utils;

import org.bukkit.enchantments.Enchantment;

/**
 * EnchantmentUtils is a utility class for managing enchantments in a Bukkit plugin.
 */
public abstract class EnchantmentUtils {

    /**
     * Get an enchantment by its name.
     *
     * @param enchantmentName The name of the enchantment.
     * @return The Enchantment object, or null if not found.
     */
    public static Enchantment getEnchantment(String enchantmentName) {
        return Enchantment.getByName(enchantmentName);
    }

}
