package me.yleoft.zAPI.utils;

import org.bukkit.enchantments.Enchantment;

public abstract class EnchantmentUtils {

    public static Enchantment getEnchantment(String enchantmentName) {
        return Enchantment.getByName(enchantmentName);
    }

}
