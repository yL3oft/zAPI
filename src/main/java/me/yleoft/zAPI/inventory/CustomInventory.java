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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.random;
import static java.util.Objects.requireNonNull;
import static me.yleoft.zAPI.utils.ConfigUtils.formPath;
import static me.yleoft.zAPI.utils.ItemStackUtils.*;
import static me.yleoft.zAPI.utils.NbtUtils.addCustomCommands;
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
        this.inventoryName = stringUtils.transform(player, requireNonNull(config.getString(formPath(configPathInventory, "title"))));
        this.rows = config.getInt(formPath(configPathInventory, "rows"));

        for(String itemPath : config.getConfigurationSection(configPathItems).getKeys(false)) {
            String materialPath = formPath(configPathItems, itemPath, "material");
            String slotPath = formPath(configPathItems, itemPath, "slot");
            ItemStack item = getItemFromConfig(player, config, formPath(configPathItems, itemPath));
            String slotS = config.getString(slotPath);
            assert slotS != null;
            setItem(config, materialPath, slotS, item, null);
        }
    }
    public CustomInventory(@NotNull YamlConfiguration config) {
        this(null, config);
    }

    /**
     * Adds an item to the inventory at the specified slot.
     * @param slot The slot to add the item to (0-53).
     * @param item The item to add.
     * @throws IllegalArgumentException if the slot is out of bounds.
     */
    public void setItem(int slot, @NotNull ItemStack item, @NotNull List<String> commands, @Nullable HashMap<String, String> replaces) {
        if (slot < 0 || slot >= rows*9) {
            throw new IllegalArgumentException("Slot must be between 0 and " + (rows*9 - 1));
        }
        if (replaces != null && !replaces.isEmpty()) {
            addCustomCommands(item, commands, replaces);
            replaceAll(item, replaces);
        }else {
            addCustomCommands(item, commands);
        }
        items.put(slot, item);
    }
    public void setItem(int slot, @NotNull ItemStack item, @NotNull String command, @Nullable HashMap<String, String> replaces) {
        setItem(slot, item, List.of(command), replaces);
    }
    public void setItem(int slot, @NotNull ItemStack item, @NotNull String command) {
        setItem(slot, item, List.of(command), null);
    }
    public void setItem(int slot, @NotNull ItemStack item, @Nullable HashMap<String, String> replaces) {
        setItem(slot, item, new ArrayList<>(), replaces);
    }
    public void setItem(int slot, @NotNull ItemStack item) {
        setItem(slot, item, new ArrayList<>(), null);
    }
    public void setItem(@Nullable YamlConfiguration config, @Nullable String materialPath, @NotNull String slot, @NotNull ItemStack item, @Nullable HashMap<String, String> replaces) {
        if (slot.matches("\\d+")) {
            setItem(Integer.parseInt(slot), item, replaces);
        } else if (slot.matches("\\d+-\\d+")) {
            String[] parts = slot.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if(config != null && materialPath != null && config.isList(materialPath)) {
                List<Material> materials = new ArrayList<>();
                config.getStringList(materialPath).forEach(m -> materials.add(MaterialUtils.getMaterial(m)));
                for (int i = start; i <= end; i++) {
                    item.setType(materials.get((int) (floor(random() * materials.size()))));
                    setItem(i, item.clone(), replaces);
                }
            }else {
                for (int i = start; i <= end; i++) {
                    setItem(i, item.clone(), replaces);
                }
            }
        }
    }
    public void setItem(@Nullable Player player, @NotNull YamlConfiguration config, @NotNull String path, @Nullable String replace, @Nullable List<String> replacers, boolean addIfEmpty) {
        String materialPath = formPath(path, "material");
        String slotPath = formPath(path, "slot");
        String slot = config.getString(slotPath);
        ItemStack item;
        assert slot != null;
        if (slot.matches("\\d+")) {
            item = getItemFromConfig(player, config, path);
            HashMap<String, String> hash = new HashMap<>();
            if (replace != null && replacers != null && !replacers.isEmpty()) {
                hash.put(replace, replacers.get(0));
                setItem(Integer.parseInt(slot), item, hash);
                return;
            }
            if(addIfEmpty) {
                setItem(Integer.parseInt(slot), item, hash);
            }
        } else if (slot.matches("\\d+-\\d+")) {
            String[] parts = slot.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if(config.isList(materialPath)) {
                List<Material> materials = new ArrayList<>();
                config.getStringList(materialPath).forEach(m -> materials.add(MaterialUtils.getMaterial(m)));
                for (int i = start; i <= end; i++) {
                    HashMap<String, String> hash = new HashMap<>();
                    if (replace != null && replacers != null && !replacers.isEmpty()) {
                        hash.put(replace, replacers.get(0));
                        item = getItemFromConfig(player, config, path, hash);
                        item.setType(materials.get((int) (floor(random() * materials.size()))));
                        setItem(i, item.clone(), hash);
                        replacers.remove(0);
                        continue;
                    }
                    if(addIfEmpty) {
                        item = getItemFromConfig(player, config, path);
                        item.setType(materials.get((int) (floor(random() * materials.size()))));
                        setItem(i, item.clone());
                    }
                }
            }else {
                for (int i = start; i <= end; i++) {
                    HashMap<String, String> hash = new HashMap<>();
                    if (replace != null && replacers != null && !replacers.isEmpty()) {
                        hash.put(replace, replacers.get(0));
                        item = getItemFromConfig(player, config, path, hash);
                        setItem(i, item.clone(), hash);
                        replacers.remove(0);
                        continue;
                    }
                    if(addIfEmpty) {
                        item = getItemFromConfig(player, config, path);
                        setItem(i, item.clone());
                    }
                }
            }
        }
    }
    public void setItem(@Nullable Player player, @NotNull YamlConfiguration config, @NotNull String path, @Nullable String replace, @Nullable List<String> replacers) {
        setItem(player, config, path, replace, replacers, false);
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
