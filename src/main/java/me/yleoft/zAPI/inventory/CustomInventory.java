package me.yleoft.zAPI.inventory;

import me.yleoft.zAPI.utils.ItemStackUtils;
import me.yleoft.zAPI.utils.MaterialUtils;
import me.yleoft.zAPI.utils.NbtUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.yleoft.zAPI.zAPI.stringUtils;

/**
 * CustomInventory class to create a custom inventory with a specified name and number of rows.
 * The inventory can hold items and can be retrieved as an Inventory object.
 */
public class CustomInventory {

    /**
     * The mark used to identify items in the inventory.
     */
    public static String mark = "zAPI:customInventory";
    public static String configPathInventory = "Inventory";
    public static String configPathItems = "Items";

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
     * Creates a custom inventory from a YamlConfiguration file.
     * @param config The YamlConfiguration file to load the inventory from.
     */
    public CustomInventory(@Nullable Player player, @NotNull YamlConfiguration config) {
        this.inventoryName = stringUtils.transform(player, Objects.requireNonNull(config.getString(formPath(configPathInventory, "title"))));
        this.rows = config.getInt(formPath(configPathInventory, "rows"));

        for(String itemPath : config.getConfigurationSection(configPathItems).getKeys(false)) {
            String materialPath = formPath(configPathItems, itemPath, "material");
            String amountPath = formPath(configPathItems, itemPath, "amount");
            String slotPath = formPath(configPathItems, itemPath, "slot");
            String namePath = formPath(configPathItems, itemPath, "name");
            String lorePath = formPath(configPathItems, itemPath, "lore");
            String commandsPath = formPath(configPathItems, itemPath, "commands");
            int amount = config.contains(amountPath) ? config.getInt(amountPath) : 1;
            ItemStack item = null;
            if(config.isList(materialPath)) {
                List<String> materials = config.getStringList(materialPath);
                item = ItemStackUtils.getItem(materials.get((int) (Math.floor(Math.random() * materials.size()))), amount);
            }else {
                item = ItemStackUtils.getItem(Objects.requireNonNull(config.getString(materialPath)), amount);
            }
            ItemMeta meta = item.getItemMeta();
            if (config.contains(namePath)) meta.setDisplayName(stringUtils.transform(player, Objects.requireNonNull(config.getString(namePath))));
            if (config.contains(lorePath)) {
                List<String> lore;
                if (config.isList(lorePath)) {
                    lore = config.getStringList(lorePath);
                    List<String> transformedLore = new ArrayList<>();
                    lore.forEach(loreLine -> transformedLore.add(stringUtils.transform(player, loreLine)));
                    lore = transformedLore;
                } else {
                    lore = List.of(stringUtils.transform(player, Objects.requireNonNull(config.getString(lorePath))));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            List<String> commands = new ArrayList<>();
            if (config.contains(commandsPath)) {
                if (config.isList(commandsPath)) {
                    commands = config.getStringList(commandsPath);
                } else {
                    commands = List.of(Objects.requireNonNull(config.getString(commandsPath)));
                }
            }
            String slotS = config.getString(slotPath);
            assert slotS != null;
            if (slotS.matches("\\d+")) {
                setItem(Integer.parseInt(slotS), item, commands);
            } else if (slotS.matches("\\d+-\\d+")) {
                String[] parts = slotS.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                if(config.isList(materialPath)) {
                    List<Material> materials = new ArrayList<>();
                    config.getStringList(materialPath).forEach(m -> materials.add(MaterialUtils.getMaterial(m)));
                    for (int i = start; i <= end; i++) {
                        item.setType(materials.get((int) (Math.floor(Math.random() * materials.size()))));
                        setItem(i, item.clone(), commands);
                    }
                }else {
                    for (int i = start; i <= end; i++) {
                        setItem(i, item, commands);
                    }
                }
            }
        }
    }
    public CustomInventory(@NotNull YamlConfiguration config) {
        this(null, config);
    }

    /**
     * Forms a yaml path from the given strings.
     * @param strs The strings to form the path from.
     * @return The formed path.
     */
    public static String formPath(String... strs) {
        StringBuilder path = new StringBuilder();

        try {
            for(String str : strs) {
                if(path.isEmpty()) {
                    path = new StringBuilder(str);
                    continue;
                }
                path.append(".").append(str);
            }
        }catch (Exception ignored) {
        }

        return path.toString();
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
        NbtUtils.addCustomCommands(item, commands);
        items.put(slot, item);
    }
    public void setItem(int slot, @NotNull ItemStack item, String command) {
        setItem(slot, item, List.of(command));
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
            NbtUtils.markItem(item, mark);
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
