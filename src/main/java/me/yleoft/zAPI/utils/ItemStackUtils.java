package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackUtils {

    public static ItemStack getItem(@NotNull String materialString, int amount) {
        Material material = MaterialUtils.getMaterial(materialString);
        return new ItemStack(material, amount);
    }

}
