package me.yleoft.zAPI.listeners;

import me.yleoft.zAPI.utils.NbtUtils;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.yleoft.zAPI.inventory.CustomInventory.mark;

/**
 * ItemListeners is a listener class for handling item interactions in inventories.
 */
public class ItemListeners extends NbtUtils implements Listener {

    /**
     * Search for custom commands in the item clicked.
     * @param event {@link InventoryClickEvent}
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(@NotNull final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || event.getCurrentItem().getAmount() < 0) {
            return;
        }
        ItemStack item = event.getCurrentItem();

        if(isMarked(item, mark)) {
            event.setCancelled(true);
        }

        List<String> commands = getCustomCommands(item);
        for(String command : commands) {
            performCommand(player, command);
        }
    }

    public void performCommand(@NotNull Player player, @NotNull String command) {
        if(command.startsWith("[CON]")) {
            command = command.replace("[CON]", "");
            zAPI.getPlugin().getServer().dispatchCommand(zAPI.getPlugin().getServer().getConsoleSender(), command);
            return;
        }else if(command.startsWith("[INV]")) {
            command = command.replace("[INV]", "");
            if(command.equalsIgnoreCase("close")) player.closeInventory();
            return;
        }
        player.performCommand(command);
    }

}
