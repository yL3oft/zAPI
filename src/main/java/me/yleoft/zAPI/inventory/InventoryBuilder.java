package me.yleoft.zAPI.inventory;

import me.yleoft.zAPI.command.Command;
import me.yleoft.zAPI.item.ItemBuilder;
import me.yleoft.zAPI.utility.PluginYAML;
import me.yleoft.zAPI.utility.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.yleoft.zAPI.configuration.Path.formPath;

/**
 * Builder class for creating custom inventories from YAML configuration.
 * Supports dynamic slot allocation, placeholders, and display conditions.
 *
 * <p>Inventory-level placeholders available to all items:
 * <ul>
 *   <li>%rows% - Number of rows in the inventory</li>
 *   <li>%title% - Title of the inventory (raw string from config)</li>
 *   <li>%command% - Command name to open this inventory</li>
 * </ul>
 *
 * <p>These placeholders are automatically available in addition to slot-specific
 * placeholders (%slot%, %currentitem%) and any custom global placeholders provided.
 */
public class InventoryBuilder {

    // ========================================
    // Configuration Keys
    // ========================================
    public static final String KEY_INVENTORY = "Inventory";
    public static final String KEY_ITEMS = "Items";
    public static final String KEY_TITLE = "title";
    public static final String KEY_ROWS = "rows";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_SLOT = "slot";
    public static final String KEY_DISPLAY_CONDITION = "display-condition";
    public static final String KEY_PLACEHOLDERS = "placeholders";

    // ========================================
    // Instance Fields
    // ========================================
    private Component title;
    private final int rows;
    private final Map<Integer, ItemStack> items;
    private final Map<String, String> globalPlaceholders;

    // ========================================
    // Command Registration
    // ========================================

    /**
     * Loads and registers a menu command from configuration.
     * If the config contains a "command" key, it creates and registers a command
     * that opens the inventory when executed.
     */
    public static void registerMenuCommand(@NotNull YamlConfiguration config) {
        if (!config.contains(KEY_COMMAND) || !config.isString(KEY_COMMAND)) {
            return;
        }

        String commandName = config.getString(KEY_COMMAND);
        if (commandName == null || PluginYAML.isCommandRegistered(commandName)) {
            return;
        }

        Component inventoryTitle = TextFormatter.transform(
                config.getString(formPath(KEY_INVENTORY, KEY_TITLE), "Inventory")
        );

        Command command = new Command() {
            @Override
            public @NotNull String name() {
                return commandName;
            }

            @Override
            public void execute(@NotNull CommandSender sender,
                                @NotNull String[] fullArgs,
                                @NotNull String[] args) {
                if (!(sender instanceof Player)) {
                    return;
                }

                Player player = (Player) sender;
                InventoryBuilder builder = new InventoryBuilder(player, config);
                player.openInventory(builder.build());
            }

            @Override
            public boolean playerOnly() {
                return true;
            }

            @Override
            public String description() {
                return "Opens " + inventoryTitle;
            }
        };

        // Note: Command registration logic would go here
    }

    // ========================================
    // Constructors
    // ========================================

    /**
     * Creates an inventory builder with a custom title and row count.
     *
     * @param title The inventory title
     * @param rows The number of rows (1-6)
     * @throws IllegalArgumentException if rows is not between 1 and 6
     */
    public InventoryBuilder(@NotNull Component title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6, got: " + rows);
        }

        this.title = title;
        this.rows = rows;
        this.items = new HashMap<>();
        this.globalPlaceholders = new HashMap<>();
    }

    /**
     * Creates an inventory builder from a YAML configuration.
     *
     * @param player The player to resolve placeholders for (can be null)
     * @param config The YAML configuration
     */
    public InventoryBuilder(@Nullable OfflinePlayer player, @NotNull YamlConfiguration config) {
        // Load inventory metadata
        String titleString = config.getString(formPath(KEY_INVENTORY, KEY_TITLE), "Inventory");
        this.title = TextFormatter.transform(player, titleString);
        this.rows = config.getInt(formPath(KEY_INVENTORY, KEY_ROWS), 3);

        if (this.rows < 1 || this.rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6, got: " + this.rows);
        }

        this.items = new HashMap<>();
        this.globalPlaceholders = new HashMap<>();

        // Load items
        loadItemsFromConfig(player, config);
    }

    /**
     * Creates an inventory builder from a YAML configuration without player context.
     */
    public InventoryBuilder(@NotNull YamlConfiguration config) {
        this(null, config);
    }

    /**
     * Creates an inventory builder from a YAML configuration with custom global placeholders.
     * These placeholders will be applied to ALL items in the inventory, including title.
     *
     * @param player The player to resolve placeholders for (can be null)
     * @param config The YAML configuration
     * @param globalPlaceholders Custom placeholders to apply globally (e.g., %targetplayer%, %page%)
     */
    public InventoryBuilder(@Nullable OfflinePlayer player,
                            @NotNull YamlConfiguration config,
                            @NotNull Map<String, String> globalPlaceholders) {
        // Load inventory metadata
        String titleString = config.getString(formPath(KEY_INVENTORY, KEY_TITLE), "Inventory");

        // Apply global placeholders to title BEFORE TextFormatter
        for (Map.Entry<String, String> entry : globalPlaceholders.entrySet()) {
            titleString = titleString.replace(entry.getKey(), entry.getValue());
        }

        this.title = TextFormatter.transform(player, titleString);
        this.rows = config.getInt(formPath(KEY_INVENTORY, KEY_ROWS), 3);

        if (this.rows < 1 || this.rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6, got: " + this.rows);
        }

        this.items = new HashMap<>();
        this.globalPlaceholders = new HashMap<>(globalPlaceholders);

        // Load items with custom placeholders
        loadItemsFromConfig(player, config);
    }

    // ========================================
    // Item Loading from Config
    // ========================================

    /**
     * Creates inventory-level placeholders that are available to all items.
     *
     * Provides placeholders for:
     * - %rows% - The number of rows in the inventory
     * - %title% - The raw title string from config (before formatting)
     * - %command% - The command name to open this inventory (if defined)
     * - Any custom placeholders defined in Inventory.placeholders section
     *
     * Custom inventory placeholders are parsed once when the inventory is created,
     * following the standard order: custom placeholders → math → PlaceholderAPI
     *
     * @param player The player for placeholder resolution (can be null)
     * @param config The YAML configuration
     * @return Map of inventory-level placeholders
     */
    @NotNull
    private Map<String, String> createInventoryPlaceholders(@Nullable OfflinePlayer player,
                                                            @NotNull YamlConfiguration config) {
        Map<String, String> placeholders = new HashMap<>();

        // %rows% - number of rows
        placeholders.put("%rows%", String.valueOf(this.rows));

        // %title% - raw title string from config (before any formatting/components)
        String titleString = config.getString(formPath(KEY_INVENTORY, KEY_TITLE), "Inventory");
        placeholders.put("%title%", titleString);

        // %command% - command name (if defined)
        String commandName = config.getString(KEY_COMMAND, "");
        placeholders.put("%command%", commandName);

        // Parse custom inventory-level placeholders from Inventory.placeholders
        // These are processed once when the inventory is created
        Map<String, String> customPlaceholders = ItemBuilder.parsePlaceholderDefinitions(
                player,
                config,
                formPath(KEY_INVENTORY, KEY_PLACEHOLDERS),
                placeholders
        );

        // Merge custom placeholders (they can override built-in ones)
        placeholders.putAll(customPlaceholders);

        return placeholders;
    }

    /**
     * Loads all items from the configuration's Items section.
     */
    private void loadItemsFromConfig(@Nullable OfflinePlayer player, @NotNull YamlConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection(KEY_ITEMS);
        if (itemsSection == null) {
            return;
        }

        // Create inventory-level placeholders and add them to global placeholders
        Map<String, String> inventoryPlaceholders = createInventoryPlaceholders(player, config);
        globalPlaceholders.putAll(inventoryPlaceholders);

        for (String itemKey : itemsSection.getKeys(false)) {
            String itemPath = formPath(KEY_ITEMS, itemKey);
            loadItemFromPath(player, config, itemPath);
        }
    }

    /**
     * Loads a single item from a configuration path and places it in the inventory.
     */
    private void loadItemFromPath(@Nullable OfflinePlayer player,
                                  @NotNull YamlConfiguration config,
                                  @NotNull String itemPath) {
        // Parse slots
        String slotPath = formPath(itemPath, KEY_SLOT);
        if (!config.contains(slotPath)) {
            return; // No slot defined
        }

        List<Integer> slots = parseSlots(player, config, slotPath);
        if (slots.isEmpty()) {
            return;
        }

        // Get display condition if it exists
        String conditionPath = formPath(itemPath, KEY_DISPLAY_CONDITION);
        String condition = config.contains(conditionPath) ? config.getString(conditionPath) : null;

        // Create and place items
        int itemIndex = 1;
        for (int slot : slots) {
            // Start with global placeholders, then add slot-specific placeholders
            // Slot placeholders will override global ones if there's a conflict
            Map<String, String> placeholders = new HashMap<>(globalPlaceholders);
            placeholders.putAll(ItemBuilder.createSlotPlaceholders(player, slot, itemIndex));

            // Parse placeholder definitions from the item's config
            // This needs to happen BEFORE display condition evaluation
            Map<String, String> parsedPlaceholders = ItemBuilder.parsePlaceholderDefinitions(
                    player,
                    config,
                    formPath(itemPath, ItemBuilder.KEY_PLACEHOLDERS),
                    placeholders
            );

            // Merge base placeholders with parsed placeholders for display condition
            Map<String, String> finalPlaceholders = ItemBuilder.mergePlaceholders(placeholders, parsedPlaceholders);

            // Check display condition for THIS specific item (with ALL placeholders applied)
            if (condition != null && !ItemBuilder.evaluateDisplayCondition(player, condition, finalPlaceholders)) {
                itemIndex++; // Still increment so next items have correct index
                continue; // Condition failed, skip this slot
            }

            // Create item with final placeholders (this will re-parse placeholders, but that's ok for consistency)
            ItemStack item = ItemBuilder.createItem(player, config, itemPath, placeholders);

            // Place item in slot
            setItem(slot, item);

            itemIndex++;
        }
    }

    // ========================================
    // Slot Parsing
    // ========================================

    /**
     * Parses slot configuration, supporting:
     * - Single number: slot: 5
     * - Range: slot: "4-12"
     * - List: slot: ["0-8", 9, "18-26", 27]
     * - Placeholders and math: slot: "{math: 9*%rows%}"
     *
     * Placeholders are processed in standard order:
     * 1. Custom placeholders (including inventory placeholders)
     * 2. Math expressions
     * 3. PlaceholderAPI placeholders
     *
     * @param player The player for placeholder resolution (can be null)
     * @param config The configuration
     * @param slotPath The path to the slot value
     * @return List of slot numbers
     */
    @NotNull
    private List<Integer> parseSlots(@Nullable OfflinePlayer player,
                                     @NotNull YamlConfiguration config,
                                     @NotNull String slotPath) {
        List<Integer> slots = new ArrayList<>();

        // Check if it's a list
        if (config.isList(slotPath)) {
            List<?> slotList = config.getList(slotPath);
            if (slotList != null) {
                for (Object slotObj : slotList) {
                    String slotStr = String.valueOf(slotObj);
                    // Apply placeholders before parsing
                    slotStr = ItemBuilder.applyPlaceholdersPublic(player, slotStr, globalPlaceholders);
                    slots.addAll(parseSlotString(slotStr));
                }
            }
        } else {
            // Single value (number or range)
            String slotStr = config.getString(slotPath);
            if (slotStr != null) {
                // Apply placeholders before parsing
                slotStr = ItemBuilder.applyPlaceholdersPublic(player, slotStr, globalPlaceholders);
                slots.addAll(parseSlotString(slotStr));
            }
        }

        return slots;
    }

    /**
     * Parses a single slot string, which can be:
     * - A number: "5"
     * - A range: "4-12"
     *
     * @param slotStr The slot string to parse
     * @return List of slot numbers
     */
    @NotNull
    private List<Integer> parseSlotString(@NotNull String slotStr) {
        List<Integer> slots = new ArrayList<>();
        String trimmed = slotStr.trim();

        // Check if it's a range (contains hyphen)
        if (trimmed.matches("\\d+\\s*-\\s*\\d+")) {
            String[] parts = trimmed.split("-");
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());

                // Add all slots in range (inclusive)
                for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                    if (isValidSlot(i)) {
                        slots.add(i);
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid range format, skip
            }
        } else {
            // Single number
            try {
                int slot = Integer.parseInt(trimmed);
                if (isValidSlot(slot)) {
                    slots.add(slot);
                }
            } catch (NumberFormatException e) {
                // Invalid number format, skip
            }
        }

        return slots;
    }

    /**
     * Checks if a slot number is valid for this inventory.
     */
    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < rows * 9;
    }

    // ========================================
    // Item Manipulation
    // ========================================

    /**
     * Sets an item in a specific slot.
     *
     * @param slot The slot number (0 to rows*9-1)
     * @param item The item to place
     * @throws IllegalArgumentException if slot is out of bounds
     */
    public void setItem(int slot, @NotNull ItemStack item) {
        if (!isValidSlot(slot)) {
            throw new IllegalArgumentException(
                    String.format("Slot %d is out of bounds (0-%d)", slot, rows * 9 - 1)
            );
        }

        items.put(slot, item.clone());
    }

    /**
     * Sets an item in a specific slot with custom placeholders.
     */
    public void setItem(int slot,
                        @NotNull ItemStack item,
                        @NotNull Map<String, String> placeholders) {
        if (!isValidSlot(slot)) {
            throw new IllegalArgumentException(
                    String.format("Slot %d is out of bounds (0-%d)", slot, rows * 9 - 1)
            );
        }

        ItemStack clonedItem = item.clone();
        ItemBuilder.replacePlaceholders(clonedItem, placeholders);
        items.put(slot, clonedItem);
    }

    /**
     * Removes an item from a specific slot.
     *
     * @param slot The slot number
     * @throws IllegalArgumentException if slot is out of bounds
     */
    public void removeItem(int slot) {
        if (!isValidSlot(slot)) {
            throw new IllegalArgumentException(
                    String.format("Slot %d is out of bounds (0-%d)", slot, rows * 9 - 1)
            );
        }

        items.remove(slot);
    }

    /**
     * Clears all items from the inventory.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Gets the item in a specific slot.
     *
     * @param slot The slot number
     * @return The item in the slot, or null if empty
     */
    @Nullable
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Checks if a slot has an item.
     */
    public boolean hasItem(int slot) {
        return items.containsKey(slot);
    }

    // ========================================
    // Inventory Properties
    // ========================================

    /**
     * Sets the inventory title.
     */
    public void setTitle(@NotNull Component title) {
        this.title = title;
    }

    /**
     * Sets the inventory title from a string.
     */
    public void setTitle(@Nullable OfflinePlayer player, @NotNull String titleString) {
        this.title = TextFormatter.transform(player, titleString);
    }

    /**
     * Gets the inventory title.
     */
    @NotNull
    public Component getTitle() {
        return title;
    }

    /**
     * Gets the number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the total number of slots.
     */
    public int getSize() {
        return rows * 9;
    }

    // ========================================
    // Inventory Building
    // ========================================

    /**
     * Builds and returns the final Bukkit Inventory.
     *
     * @return The created inventory with all items placed
     */
    @NotNull
    public Inventory build() {
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);

        // Place all items
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }

        return inventory;
    }

    /**
     * Legacy method name for compatibility.
     */
    @NotNull
    public Inventory getInventory() {
        return build();
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Gets a map of all items in the inventory.
     *
     * @return Unmodifiable map of slot to item
     */
    @NotNull
    public Map<Integer, ItemStack> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Gets a count of how many items are in the inventory.
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Checks if the inventory is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Creates a copy of this inventory builder.
     */
    @NotNull
    public InventoryBuilder copy() {
        InventoryBuilder copy = new InventoryBuilder(this.title, this.rows);

        for (Map.Entry<Integer, ItemStack> entry : this.items.entrySet()) {
            copy.items.put(entry.getKey(), entry.getValue().clone());
        }

        return copy;
    }

    @Override
    public String toString() {
        return String.format("InventoryBuilder{title=%s, rows=%d, items=%d}",
                title, rows, items.size());
    }
}