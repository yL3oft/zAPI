package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public abstract class MaterialUtils {

    public static Material getMaterial(@NotNull String materialString) {
        if(StringUtils.startsWithIgnoreCase(materialString, "[head]")) {
            return Material.PLAYER_HEAD;
        }
        Material material = Material.getMaterial(materialString);
        return material;
    }

}
