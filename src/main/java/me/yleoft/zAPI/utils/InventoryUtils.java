package me.yleoft.zAPI.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * InventoryUtils is a utility class for managing player inventories.
 */
public abstract class InventoryUtils {

    /**
     * Searches for a marked item in the player's inventory and remove it if found.
     * @param player The player whose inventory will be searched.
     */
    public static void cleanInventory(@NotNull final Player player, @NotNull final String mark) {
        for (ItemStack item : player.getInventory().getContents()) {
            if(item == null) continue;
            if(!NbtUtils.isMarked(item, mark)) continue;
            player.getInventory().remove(item);
        }
    }

}
