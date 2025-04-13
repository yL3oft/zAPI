package me.yleoft.zAPI.inventory;

import me.yleoft.zAPI.utils.NbtUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * CustomInventory class to create a custom inventory with a specified name and number of rows.
 * The inventory can hold items and can be retrieved as an Inventory object.
 */
public class CustomInventory extends NbtUtils {

    /**
     * The mark used to identify items in the inventory.
     */
    public static String mark = "zAPI:customInventory";

    private final String inventoryName;
    private final int rows;
    private HashMap<Integer, ItemStack> items = new HashMap<>();

    /**
     * Creates a custom inventory with the specified name and number of rows.
     * @param inventoryName The name of the inventory.
     * @param rows The number of rows (1-6).
     * @throws IllegalArgumentException if the number of rows is not between 1 and 6.
     */
    public CustomInventory(@NotNull String inventoryName, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Number of rows must be between 1 and 6");
        }
        this.inventoryName = inventoryName;
        this.rows = rows;
    }

    /**
     * Adds an item to the inventory at the specified slot.
     * @param slot The slot to add the item to (0-53).
     * @param item The item to add.
     * @throws IllegalArgumentException if the slot is out of bounds.
     */
    public void setItem(int slot, @NotNull ItemStack item, @NotNull List<String> commands) {
        if (slot < 0 || slot >= rows*9) {
            throw new IllegalArgumentException("Slot must be between 0 and " + (rows*9 - 1));
        }
        addCustomCommands(item, commands);
        items.put(slot, item);
    }
    public void setItem(int slot, @NotNull ItemStack item) {
        setItem(slot, item, new ArrayList<>());
    }

    /**
     * Removes an item from the inventory at the specified slot.
     * @param slot The slot to remove the item from (0-53).
     * @throws IllegalArgumentException if the slot is out of bounds.
     */
    public void removeItem(int slot) {
        if (slot < 0 || slot >= rows*9) {
            throw new IllegalArgumentException("Slot must be between 0 and " + (rows*9 - 1));
        }
        items.remove(slot);
    }

    public String getInventoryName() {
        return inventoryName;
    }
    public int getRows() {
        return rows;
    }

    /**
     * Transforms the items HashMap into an array of ItemStacks.
     * @return The array of ItemStacks.
     */
    private ItemStack[] getItemsArray() {
        ItemStack[] itemsArray  = new ItemStack[rows*9];
        for (int i = 0; i < itemsArray.length; i++) {
            ItemStack item = items.getOrDefault(i, null);
            if(item == null) {
                itemsArray[i] = null;
                continue;
            }
            markItem(item, mark);
            itemsArray[i] = item;
        }
        return itemsArray;
    }

    /**
     * Creates and returns the final inventory
     * @return The created inventory.
     */
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(null, rows*9, inventoryName);
        inv.setContents(getItemsArray());
        return inv;
    }

}
