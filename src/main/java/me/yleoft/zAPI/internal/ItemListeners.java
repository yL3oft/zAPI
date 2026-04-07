package me.yleoft.zAPI.internal;

import me.yleoft.zAPI.item.NbtHandler;
import me.yleoft.zAPI.player.PlayerHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * ItemListeners is a listener class for handling item interactions in inventories.
 */
public class ItemListeners implements Listener {

    /**
     * Search for custom commands in the item clicked.
     * @param event {@link InventoryClickEvent}
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(@NotNull final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || event.getCurrentItem().getAmount() < 0) {
            return;
        }
        ItemStack item = event.getCurrentItem();

        if(NbtHandler.hasMark(item, NbtHandler.mark)) {
            if (NbtHandler.isMarked(item, NbtHandler.mark)) {
                event.setCancelled(true);
            }

            List<String> commands = NbtHandler.getCustomCommands(item);
            PlayerHandler.performCommand(player, item, commands);
        }
    }

}
