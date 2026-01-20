package me.yleoft.zAPI.listeners;

import me.yleoft.zAPI.item.NbtHandler;
import me.yleoft.zAPI.utility.scheduler.Scheduler;
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
public class DupeFixerListeners extends NbtHandler implements Listener {

    /**
     * Searches for a marked item in the player's inventory and removes it if found.
     */
    @EventHandler
    private void onClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Scheduler.runTaskLater(player.getLocation(), () -> cleanInventory(player, mark), 3L);
    }

    /**
     * Checks if the item being picked up is marked for duplication and removes it if so.
     */
    @EventHandler
    private void onPickup(@NotNull final EntityPickupItemEvent event) {
        if (!isMarked(event.getItem().getItemStack(), mark)) {
            return;
        }

        event.getItem().remove();
    }

    /**
     * Checks if the item being dropped is marked for duplication and removes it if so.
     */
    @EventHandler
    private void onDrop(@NotNull final PlayerDropItemEvent event) {
        if (!isMarked(event.getItemDrop().getItemStack(), mark)) {
            return;
        }

        event.getItemDrop().remove();
    }

    /**
     * Cleans the player's inventory of marked items after a short delay upon login.
     */
    @EventHandler
    private void onLogin(@NotNull final PlayerLoginEvent event) {
        Scheduler.runTaskLater(event.getPlayer().getLocation(), () -> cleanInventory(event.getPlayer(), mark), 10L);
    }

}
