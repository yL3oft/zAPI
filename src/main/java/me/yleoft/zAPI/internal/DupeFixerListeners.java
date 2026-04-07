package me.yleoft.zAPI.internal;

import me.yleoft.zAPI.item.NbtHandler;
import me.yleoft.zAPI.zAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;

/**
 * DupeFixerListeners is a listener class that handles various events related to item duplication.
 * It prevents players from picking up or dropping items that are marked for duplication.
 */
public class DupeFixerListeners implements Listener {

    /**
     * Searches for a marked item in the player's inventory and removes it if found.
     */
    @EventHandler
    private void onClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        zAPI.getScheduler().runAtEntityLater(player, () -> NbtHandler.cleanInventory(player, NbtHandler.mark), 3L);
    }

    /**
     * Checks if the item being picked up is marked for duplication and removes it if so.
     */
    @EventHandler
    private void onPickup(@NotNull final EntityPickupItemEvent event) {
        if (!NbtHandler.isMarked(event.getItem().getItemStack(), NbtHandler.mark)) {
            return;
        }

        event.getItem().remove();
    }

    /**
     * Checks if the item being dropped is marked for duplication and removes it if so.
     */
    @EventHandler
    private void onDrop(@NotNull final PlayerDropItemEvent event) {
        if (!NbtHandler.isMarked(event.getItemDrop().getItemStack(), NbtHandler.mark)) {
            return;
        }

        event.getItemDrop().remove();
    }

    /**
     * Cleans the player's inventory of marked items after a short delay upon login.
     */
    @EventHandler
    private void onLogin(@NotNull final PlayerLoginEvent event) {
        zAPI.getScheduler().runAtEntityLater(event.getPlayer(), () -> NbtHandler.cleanInventory(event.getPlayer(), NbtHandler.mark), 10L);
    }

}
