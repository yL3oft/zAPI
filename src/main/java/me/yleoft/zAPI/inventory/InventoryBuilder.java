package me.yleoft.zAPI.inventory;

import me.yleoft.zAPI.command.Command;
import me.yleoft.zAPI.item.ItemBuilder;
import me.yleoft.zAPI.item.NbtHandler;
import me.yleoft.zAPI.utility.PluginYAML;
import me.yleoft.zAPI.utility.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
import static me.yleoft.zAPI.configuration.Path.formPath;

/**
 * CustomInventory class to create a custom inventory with a specified name and number of rows.
 * The inventory can hold items and can be retrieved as an Inventory object.
 */
public class InventoryBuilder {

    public static final String configPathInventory = "Inventory";
    public static final String configPathItems = "Items";

    private YamlConfiguration config;
    private Component inventoryName;
    private final int rows;
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public static void loadMenuCommand(@NotNull YamlConfiguration config) {
        if(config.contains("command") && config.isString("command")) {
            Component inventoryName = TextFormatter.transform(requireNonNull(config.getString(formPath(configPathInventory, "title"))));
            String command = config.getString("command");
            if(!PluginYAML.isCommandRegistered(command)) {
                Command commandObj = new Command() {
                    @Override
                    public @NotNull String name() {
                        return command;
                    }

                    @Override
                    public void execute(@NotNull CommandSender sender, @NotNull String[] fullArgs, @NotNull String @NotNull [] args) {
                        Player p = (Player) sender;
                        InventoryBuilder inventory = new InventoryBuilder(p, config);
                        p.openInventory(inventory.getInventory());
                    }

                    @Override
                    public boolean playerOnly() {
                        return true;
                    }

                    @Override
                    public String description() {
                        return "Opens the custom inventory " + inventoryName;
                    }
                };
            }
        }
    }

    /**
     * Creates a custom inventory with the specified name and number of rows.
     * @param inventoryName The name of the inventory.
     * @param rows The number of rows (1-6).
     * @throws IllegalArgumentException if the number of rows is not between 1 and 6.
     */
    public InventoryBuilder(@NotNull Component inventoryName, int rows) {
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
    public InventoryBuilder(@Nullable Player player, @NotNull YamlConfiguration config) {
        this.config = config;
        this.inventoryName = TextFormatter.transform(player, requireNonNull(config.getString(formPath(configPathInventory, "title"))));
        this.rows = config.getInt(formPath(configPathInventory, "rows"), 3);

        for(String itemPath : config.getConfigurationSection(configPathItems).getKeys(false)) {
            String materialPath = formPath(configPathItems, itemPath, "material");
            String slotPath = formPath(configPathItems, itemPath, "slot");
            ItemStack item = ItemBuilder.getItemFromConfig(player, config, formPath(configPathItems, itemPath));
            String slotS = config.getString(slotPath);
            assert slotS != null;
            setItem(config, materialPath, slotS, item, null);
        }
    }
    public InventoryBuilder(@NotNull YamlConfiguration config) {
        this(null, config);
    }

    /**
     * Adds an item to the inventory at the specified slot.
     * @param slot The slot to add the item to (0-53).
     * @param item The item to add.
     * @throws IllegalArgumentException if the slot is out of bounds.
     */
    public void setItem(int slot, @NotNull ItemStack item, @NotNull List<String> commands, @Nullable Map<String, String> replaces) {
        if (slot < 0 || slot >= rows*9) {
            throw new IllegalArgumentException("Slot must be between 0 and " + (rows*9 - 1));
        }
        if (replaces != null && !replaces.isEmpty()) {
            NbtHandler.addCustomCommands(item, commands, replaces);
            ItemBuilder.replaceAllString(item, replaces);
        }else {
            NbtHandler.addCustomCommands(item, commands);
        }
        items.put(slot, item);
    }
    public void setItem(int slot, @NotNull ItemStack item, @NotNull String command, @Nullable Map<String, String> replaces) {
        setItem(slot, item, Collections.singletonList(command), replaces);
    }
    public void setItem(int slot, @NotNull ItemStack item, @NotNull String command) {
        setItem(slot, item, Collections.singletonList(command), null);
    }
    public void setItem(int slot, @NotNull ItemStack item, @Nullable Map<String, String> replaces) {
        setItem(slot, item, new ArrayList<>(), replaces);
    }
    public void setItem(int slot, @NotNull ItemStack item) {
        setItem(slot, item, new ArrayList<>(), null);
    }
    public void setItem(@Nullable YamlConfiguration config, @Nullable String materialPath, @NotNull String slot, @NotNull ItemStack item, @Nullable Map<String, String> replaces) {
        if (slot.matches("\\d+")) {
            setItem(Integer.parseInt(slot), item, replaces);
        } else if (slot.matches("\\d+-\\d+")) {
            String[] parts = slot.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if(config != null && materialPath != null && config.isList(materialPath)) {
                List<Material> materials = new ArrayList<>();
                config.getStringList(materialPath).forEach(m -> materials.add(ItemBuilder.getMaterial(m)));
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
            Map<String, String> hash = new HashMap<>();
            if (replace != null && replacers != null && !replacers.isEmpty()) {
                hash.put(replace, replacers.get(0));
                item = ItemBuilder.getItemFromConfigString(player, config, path, hash);
                setItem(Integer.parseInt(slot), item, hash);
                return;
            }
            if(addIfEmpty) {
                item = ItemBuilder.getItemFromConfig(player, config, path);
                setItem(Integer.parseInt(slot), item, hash);
            }
        } else if (slot.matches("\\d+-\\d+")) {
            String[] parts = slot.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if(config.isList(materialPath)) {
                List<Material> materials = new ArrayList<>();
                config.getStringList(materialPath).forEach(m -> materials.add(ItemBuilder.getMaterial(m)));
                for (int i = start; i <= end; i++) {
                    Map<String, String> hash = new HashMap<>();
                    if (replace != null && replacers != null && !replacers.isEmpty()) {
                        hash.put(replace, replacers.get(0));
                        item = ItemBuilder.getItemFromConfigString(player, config, path, hash);
                        item.setType(materials.get((int) (floor(random() * materials.size()))));
                        setItem(i, item.clone(), hash);
                        replacers.remove(0);
                        continue;
                    }
                    if(addIfEmpty) {
                        item = ItemBuilder.getItemFromConfig(player, config, path);
                        item.setType(materials.get((int) (floor(random() * materials.size()))));
                        setItem(i, item.clone());
                    }
                }
            }else {
                for (int i = start; i <= end; i++) {
                    Map<String, String> hash = new HashMap<>();
                    if (replace != null && replacers != null && !replacers.isEmpty()) {
                        hash.put(replace, replacers.get(0));
                        item = ItemBuilder.getItemFromConfigString(player, config, path, hash);
                        setItem(i, item.clone(), hash);
                        replacers.remove(0);
                        continue;
                    }
                    if(addIfEmpty) {
                        item = ItemBuilder.getItemFromConfig(player, config, path);
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

    /**
     * Sets the name of the inventory.
     * @param inventoryName The name of the inventory.
     */
    public void setInventoryName(@Nullable OfflinePlayer player, @NotNull String inventoryName) {
        this.inventoryName = TextFormatter.transform(player, inventoryName);
    }public void setInventoryName(@NotNull String inventoryName) {
        setInventoryName(null, inventoryName);
    }

    /**
     * Gets the name of the inventory.
     * @return The name of the inventory.
     */
    @NotNull
    public Component getInventoryName() {
        return inventoryName;
    }

    /**
     * Gets the number of rows in the inventory.
     * @return The number of rows in the inventory.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Transforms the items Map into an array of ItemStacks.
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
            itemsArray[i] = item;
        }
        return itemsArray;
    }

    /**
     * Creates and returns the final inventory
     * @return The created inventory.
     */
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(null, rows*9, TextFormatter.transform(inventoryName));
        inv.setContents(getItemsArray());
        return inv;
    }

}
