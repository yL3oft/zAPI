package me.yleoft.zAPI.item;

import me.yleoft.zAPI.skull.HeadProvider;
import me.yleoft.zAPI.utility.MathExpressionEvaluator;
import me.yleoft.zAPI.utility.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.yleoft.zAPI.configuration.Path.formPath;
import static me.yleoft.zAPI.item.NbtHandler.mark;

/**
 * Utility class for building ItemStacks from configuration files.
 * Handles item creation with support for placeholders, conditions, and dynamic slot allocation.
 */
public abstract class ItemBuilder {

    // ========================================
    // Configuration Keys
    // ========================================
    public static final String KEY_MATERIAL = "material";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_NAME = "name";
    public static final String KEY_LORE = "lore";
    public static final String KEY_ENCHANTMENTS = "enchantments";
    public static final String KEY_UNBREAKABLE = "unbreakable";
    public static final String KEY_ITEM_FLAGS = "itemflags";
    public static final String KEY_PICKABLE = "pickable";
    public static final String KEY_COMMANDS = "commands";
    public static final String KEY_DISPLAY_CONDITION = "display-condition";
    public static final String KEY_PLACEHOLDERS = "placeholders";

    // ========================================
    // Item Creation from Config
    // ========================================

    /**
     * Creates an ItemStack from a configuration path with placeholder support.
     *
     * @param player The player for placeholder resolution (can be null)
     * @param config The YAML configuration containing the item definition
     * @param path The configuration path to the item
     * @param placeholders Additional placeholders to apply (can be null)
     * @return The created ItemStack
     */
    @NotNull
    public static ItemStack createItem(@Nullable OfflinePlayer player,
                                       @NotNull YamlConfiguration config,
                                       @NotNull String path,
                                       @Nullable Map<String, String> placeholders) {

        // Parse placeholder definitions from config first
        // These get evaluated once with the base placeholders and then reused
        Map<String, String> parsedPlaceholders = parsePlaceholderDefinitions(
                player,
                config,
                formPath(path, KEY_PLACEHOLDERS),
                placeholders
        );

        // Merge: base placeholders + parsed placeholders
        // Parsed placeholders will override base placeholders if there's a conflict
        Map<String, String> finalPlaceholders = mergePlaceholders(placeholders, parsedPlaceholders);

        // Load material with parsed placeholders
        Material material = loadMaterial(player, config, formPath(path, KEY_MATERIAL), finalPlaceholders);
        int amount = config.getInt(formPath(path, KEY_AMOUNT), 1);
        ItemStack item = createItemFromMaterial(player, material.name(), amount);

        // Apply metadata
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyName(meta, player, config, formPath(path, KEY_NAME), finalPlaceholders);
            applyLore(meta, player, config, formPath(path, KEY_LORE), finalPlaceholders);
            applyEnchantments(meta, player, config, formPath(path, KEY_ENCHANTMENTS));
            applyUnbreakable(meta, config, formPath(path, KEY_UNBREAKABLE));
            applyItemFlags(meta, player, config, formPath(path, KEY_ITEM_FLAGS));
            item.setItemMeta(meta);
        }

        // Apply NBT data
        applyPickable(item, config, formPath(path, KEY_PICKABLE));
        applyCommands(item, player, config, formPath(path, KEY_COMMANDS), finalPlaceholders);

        return item;
    }

    /**
     * Creates an ItemStack from a configuration path without placeholders.
     */
    @NotNull
    public static ItemStack createItem(@Nullable Player player,
                                       @NotNull YamlConfiguration config,
                                       @NotNull String path) {
        return createItem(player, config, path, null);
    }

    // ========================================
    // Material Handling
    // ========================================

    /**
     * Loads a material from config, supporting lists (random selection) and special head formats.
     */
    @NotNull
    private static Material loadMaterial(@Nullable OfflinePlayer player,
                                         @NotNull YamlConfiguration config,
                                         @NotNull String path,
                                         @Nullable Map<String, String> placeholders) {
        if (!config.contains(path)) {
            return Material.STONE;
        }

        String materialString;
        if (config.isList(path)) {
            List<String> materials = config.getStringList(path);
            materialString = materials.isEmpty() ? "STONE" : materials.get(new Random().nextInt(materials.size()));
        } else {
            materialString = config.getString(path, "STONE");
        }

        // Apply placeholders to material string (e.g., material: "%material%")
        materialString = applyPlaceholders(player, materialString, placeholders);

        return parseMaterial(materialString);
    }

    /**
     * Loads a material from config without placeholders (legacy method).
     */
    @NotNull
    private static Material loadMaterial(@Nullable OfflinePlayer player,
                                         @NotNull YamlConfiguration config,
                                         @NotNull String path) {
        return loadMaterial(player, config, path, null);
    }

    /**
     * Parses a material string, handling special head formats.
     */
    @NotNull
    public static Material parseMaterial(@NotNull String materialString) {
        if (materialString.startsWith("head-") ||
                materialString.startsWith("base64head-") ||
                materialString.startsWith("namehead-") ||
                materialString.startsWith("urlhead-")) {
            return Material.PLAYER_HEAD;
        }

        Material material = Material.getMaterial(materialString.toUpperCase());
        return material != null ? material : Material.STONE;
    }

    /**
     * Creates an ItemStack from a material string, handling special head formats.
     */
    @NotNull
    public static ItemStack createItemFromMaterial(@Nullable OfflinePlayer player,
                                                   @NotNull String materialString,
                                                   int amount) {
        if (materialString.startsWith("head-") ||
                materialString.startsWith("base64head-") ||
                materialString.startsWith("namehead-") ||
                materialString.startsWith("urlhead-")) {

            String[] split = materialString.split("-", 2);
            String value = TextFormatter.applyPlaceholders(player, split[1]);
            ItemStack head = HeadProvider.getPlayerHeadFromString(split[0], value);
            head.setAmount(amount);
            return head;
        }

        Material material = parseMaterial(materialString);
        return new ItemStack(material, amount);
    }

    // ========================================
    // Metadata Application
    // ========================================

    /**
     * Applies display name to item meta with placeholder support.
     */
    private static void applyName(@NotNull ItemMeta meta,
                                  @Nullable OfflinePlayer player,
                                  @NotNull YamlConfiguration config,
                                  @NotNull String path,
                                  @Nullable Map<String, String> placeholders) {
        if (!config.contains(path)) return;

        String nameString = config.getString(path);
        if (nameString != null) {
            nameString = applyPlaceholders(player, nameString, placeholders);
            meta.itemName(TextFormatter.transform(player, nameString));
        }
    }

    /**
     * Applies lore to item meta with placeholder support.
     */
    private static void applyLore(@NotNull ItemMeta meta,
                                  @Nullable OfflinePlayer player,
                                  @NotNull YamlConfiguration config,
                                  @NotNull String path,
                                  @Nullable Map<String, String> placeholders) {
        if (!config.contains(path)) return;

        List<String> loreStrings = config.isList(path)
                ? config.getStringList(path)
                : Collections.singletonList(config.getString(path, ""));

        List<Component> lore = loreStrings.stream()
                .map(line -> applyPlaceholders(player, line, placeholders))
                .map(line -> TextFormatter.transform(player, line))
                .map(ItemBuilder::disableDefaultItalics)
                .collect(Collectors.toList());

        meta.lore(lore);
    }

    /**
     * Applies enchantments to item meta with placeholder support.
     */
    private static void applyEnchantments(@NotNull ItemMeta meta,
                                          @Nullable OfflinePlayer player,
                                          @NotNull YamlConfiguration config,
                                          @NotNull String path) {
        List<String> enchantmentStrings = getConfigList(config, path);

        for (String enchantStr : enchantmentStrings) {
            // Apply player placeholders to enchantment string
            enchantStr = TextFormatter.applyPlaceholders(player, enchantStr);

            String[] parts = enchantStr.split(":");
            if (parts.length != 2) continue;

            String enchantName = parts[0].toUpperCase();
            Enchantment enchantment = Enchantment.getByName(enchantName);

            if (enchantment != null && isInteger(parts[1])) {
                int level = Integer.parseInt(parts[1].trim());
                meta.addEnchant(enchantment, level, true);
            }
        }
    }

    /**
     * Applies unbreakable flag to item meta.
     */
    private static void applyUnbreakable(@NotNull ItemMeta meta,
                                         @NotNull YamlConfiguration config,
                                         @NotNull String path) {
        if (config.contains(path)) {
            meta.setUnbreakable(config.getBoolean(path));
        }
    }

    /**
     * Applies item flags to item meta with placeholder support.
     */
    private static void applyItemFlags(@NotNull ItemMeta meta,
                                       @Nullable OfflinePlayer player,
                                       @NotNull YamlConfiguration config,
                                       @NotNull String path) {
        List<String> flagStrings = getConfigList(config, path);

        for (String flagStr : flagStrings) {
            // Apply player placeholders to flag string
            flagStr = TextFormatter.applyPlaceholders(player, flagStr);

            try {
                ItemFlag flag = ItemFlag.valueOf(flagStr.toUpperCase());
                meta.addItemFlags(flag);
            } catch (IllegalArgumentException e) {
                // Invalid flag name, skip
            }
        }
    }

    // ========================================
    // NBT Application
    // ========================================

    /**
     * Applies pickable NBT marker to item.
     */
    private static void applyPickable(@NotNull ItemStack item,
                                      @NotNull YamlConfiguration config,
                                      @NotNull String path) {
        boolean pickable = config.getBoolean(path, false);
        NbtHandler.markItem(item, mark, !pickable);
    }

    /**
     * Applies custom commands to item NBT with placeholder support.
     */
    private static void applyCommands(@NotNull ItemStack item,
                                      @Nullable OfflinePlayer player,
                                      @NotNull YamlConfiguration config,
                                      @NotNull String path,
                                      @Nullable Map<String, String> placeholders) {
        List<String> commands = getConfigList(config, path);

        if (commands.isEmpty()) return;

        // Apply placeholders to commands
        List<String> processedCommands = commands.stream()
                .map(cmd -> applyPlaceholders(player, cmd, placeholders))
                .collect(Collectors.toList());

        NbtHandler.addCustomCommands(item, processedCommands);
    }

    // ========================================
    // Placeholder System
    // ========================================

    /**
     * Applies placeholders to a string, including player placeholders, custom placeholders,
     * and evaluates mathematical expressions wrapped in {math: ... } tags.
     *
     * Processing order:
     * 1. Custom placeholders (e.g., %slot%, %player%, %currentitem%)
     * 2. Math expressions wrapped in {math: ... } tags
     * 3. PlaceholderAPI placeholders
     *
     * Examples:
     * - "{math: %homes_limit%*2}" -> evaluates to the result of homes_limit * 2
     * - "You have {math: %balance%/100} hundred dollars" -> evaluates the division
     * - "{math: sqrt(%player_level%)}" -> evaluates square root
     */
    @NotNull
    private static String applyPlaceholders(@Nullable OfflinePlayer player,
                                            @NotNull String text,
                                            @Nullable Map<String, String> placeholders) {
        String result = text;

        // 1. Apply custom placeholders FIRST (slot, player, currentitem, etc.)
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }

        // 2. Evaluate math expressions AFTER custom placeholders but BEFORE PlaceholderAPI
        result = evaluateMathExpressions(result);

        // 3. Apply player placeholders (PlaceholderAPI) LAST
        result = TextFormatter.applyPlaceholders(player, result);

        return result;
    }

    /**
     * Public version of applyPlaceholders for use by InventoryBuilder.
     * This allows InventoryBuilder to process slot strings with placeholders and math.
     *
     * @param player The player for placeholder resolution
     * @param text The text to process
     * @param placeholders Custom placeholders to apply
     * @return The processed text
     */
    @NotNull
    public static String applyPlaceholdersPublic(@Nullable OfflinePlayer player,
                                                 @NotNull String text,
                                                 @Nullable Map<String, String> placeholders) {
        return applyPlaceholders(player, text, placeholders);
    }

    /**
     * Evaluates all mathematical expressions in a string that are wrapped in {math: ... } tags.
     *
     * Pattern: {math: expression}
     *
     * @param text The text containing potential math expressions
     * @return The text with all math expressions evaluated and replaced
     */
    @NotNull
    private static String evaluateMathExpressions(@NotNull String text) {
        // Pattern to match {math: ... }
        Pattern pattern = Pattern.compile("\\{math:\\s*([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();

            try {
                // Evaluate the mathematical expression
                double value = MathExpressionEvaluator.evaluate(expression);

                // Format the result
                String replacement = formatMathResult(value);

                // Replace the {math: ... } with the result
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                // If evaluation fails, leave the original text
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Formats a mathematical result for display.
     * If the result is a whole number, returns it without decimal points.
     * Otherwise, returns it with up to 2 decimal places.
     */
    @NotNull
    private static String formatMathResult(double value) {
        // Check if it's a whole number
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }

        // Otherwise, format with up to 2 decimal places, removing trailing zeros
        String formatted = String.format("%.2f", value);

        // Remove trailing zeros after decimal point
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
        }

        return formatted;
    }

    /**
     * Creates a placeholder map for slot-based item generation.
     *
     * Provides placeholders for:
     * - %slot% - The slot number (0-based index)
     * - %currentitem% - The item index in a multi-slot item (1-based)
     *
     * @param player The player (can be null)
     * @param slot The slot number (0-based)
     * @param itemIndex The item index (1-based, for when an item is spread across multiple slots)
     * @return Map of slot-related placeholders
     */
    @NotNull
    public static Map<String, String> createSlotPlaceholders(@Nullable OfflinePlayer player,
                                                             int slot,
                                                             int itemIndex) {
        Map<String, String> placeholders = new HashMap<>();

        // %slot% - the actual slot number (0-based)
        placeholders.put("%slot%", String.valueOf(slot));

        // %currentitem% - index for items spread across multiple slots (1-based)
        placeholders.put("%currentitem%", String.valueOf(itemIndex));

        // %player% - player name or empty string
        placeholders.put("%player%", player != null ? player.getName() : "");

        // %online% - number of online players
        placeholders.put("%online%", String.valueOf(org.bukkit.Bukkit.getOnlinePlayers().size()));

        // %uuid% - player UUID as string or empty string
        placeholders.put("%uuid%", player != null ? player.getUniqueId().toString() : "");

        // %world% - player's current world name or empty string
        if (player != null && player.isOnline() && player instanceof Player) {
            placeholders.put("%world%", ((Player) player).getWorld().getName());
        } else {
            placeholders.put("%world%", "");
        }

        return placeholders;
    }

    /**
     * Merges multiple placeholder maps together.
     */
    @NotNull
    public static Map<String, String> mergePlaceholders(@Nullable Map<String, String>... maps) {
        Map<String, String> merged = new HashMap<>();

        for (Map<String, String> map : maps) {
            if (map != null) {
                merged.putAll(map);
            }
        }

        return merged;
    }

    /**
     * Parses placeholder definitions from the config and processes them once.
     * The placeholders are evaluated following the standard order:
     * 1. Custom placeholders (from basePlaceholders parameter)
     * 2. Math expressions
     * 3. PlaceholderAPI placeholders
     *
     * This allows you to define placeholders in the config that get parsed once
     * and then reused across name, lore, commands, etc.
     *
     * Example config:
     * <pre>
     * item:
     *   name: "%parsedplaceholder%"
     *   lore:
     *     - "Value: %parsedplaceholder%"
     *   placeholders:
     *     parsedplaceholder: "Player #%currentitem% - %player_name%"
     * </pre>
     *
     * @param player The player for placeholder resolution (can be null)
     * @param config The YAML configuration
     * @param path The configuration path to the placeholders section
     * @param basePlaceholders Base placeholders to use when parsing (e.g., %slot%, %currentitem%)
     * @return Map of parsed placeholders ready to use
     */
    @NotNull
    public static Map<String, String> parsePlaceholderDefinitions(@Nullable OfflinePlayer player,
                                                                  @NotNull YamlConfiguration config,
                                                                  @NotNull String path,
                                                                  @Nullable Map<String, String> basePlaceholders) {
        Map<String, String> parsedPlaceholders = new HashMap<>();

        // Check if placeholders section exists
        if (!config.contains(path) || !config.isConfigurationSection(path)) {
            return parsedPlaceholders;
        }

        ConfigurationSection placeholdersSection = config.getConfigurationSection(path);
        if (placeholdersSection == null) {
            return parsedPlaceholders;
        }

        // Parse each placeholder definition
        for (String key : placeholdersSection.getKeys(false)) {
            String value = placeholdersSection.getString(key);
            if (value != null) {
                // Add the % wrapping to the key for consistency
                String placeholderKey = key.startsWith("%") ? key : "%" + key + "%";

                // Parse the value following the standard order:
                // 1. Custom placeholders (basePlaceholders)
                // 2. Math expressions
                // 3. PlaceholderAPI
                String parsedValue = applyPlaceholders(player, value, basePlaceholders);

                parsedPlaceholders.put(placeholderKey, parsedValue);
            }
        }

        return parsedPlaceholders;
    }

    // ========================================
    // Display Condition Evaluation
    // ========================================

    /**
     * Evaluates a display condition, supporting both comparison operators and mathematical expressions.
     *
     * Mathematical operations supported:
     * - Basic: *, /, +, -
     * - Functions: sqrt(), round(), roundDown()
     *
     * Example conditions:
     * - "%homes_limit%*%zhomes_limit_multiplier%>=30"
     * - "sqrt(%player_level%)+5>10"
     * - "round(%value%/2)==5"
     *
     * @param player The player for placeholder resolution
     * @param condition The condition string to evaluate
     * @param placeholders Additional placeholders to apply
     * @return true if the condition is met, false otherwise
     */
    public static boolean evaluateDisplayCondition(@Nullable OfflinePlayer player,
                                                   @NotNull String condition,
                                                   @Nullable Map<String, String> placeholders) {
        // Apply placeholders to condition
        String processed = applyPlaceholders(player, condition, placeholders);

        // Try to find comparison operators
        // Check two-character operators first to avoid false matches

        // Check !=
        int notEqualsIndex = findOperatorIndex(processed, "!=");
        if (notEqualsIndex != -1) {
            String left = processed.substring(0, notEqualsIndex).trim();
            String right = processed.substring(notEqualsIndex + 2).trim();

            // Evaluate both sides as math expressions if they contain operators
            String leftEvaluated = evaluateMathIfNeeded(left);
            String rightEvaluated = evaluateMathIfNeeded(right);

            return !leftEvaluated.equals(rightEvaluated);
        }

        // Check ==
        int equalsIndex = findOperatorIndex(processed, "==");
        if (equalsIndex != -1) {
            String left = processed.substring(0, equalsIndex).trim();
            String right = processed.substring(equalsIndex + 2).trim();

            // Evaluate both sides as math expressions if they contain operators
            String leftEvaluated = evaluateMathIfNeeded(left);
            String rightEvaluated = evaluateMathIfNeeded(right);

            return leftEvaluated.equals(rightEvaluated);
        }

        // Check >=
        int greaterEqualsIndex = findOperatorIndex(processed, ">=");
        if (greaterEqualsIndex != -1) {
            String left = processed.substring(0, greaterEqualsIndex).trim();
            String right = processed.substring(greaterEqualsIndex + 2).trim();

            // Evaluate both sides as math expressions
            double leftValue = evaluateMathExpression(left);
            double rightValue = evaluateMathExpression(right);

            return leftValue >= rightValue;
        }

        // Check <=
        int lessEqualsIndex = findOperatorIndex(processed, "<=");
        if (lessEqualsIndex != -1) {
            String left = processed.substring(0, lessEqualsIndex).trim();
            String right = processed.substring(lessEqualsIndex + 2).trim();

            // Evaluate both sides as math expressions
            double leftValue = evaluateMathExpression(left);
            double rightValue = evaluateMathExpression(right);

            return leftValue <= rightValue;
        }

        // Check single-character operators (only if two-char versions weren't found)
        // Check >
        int greaterIndex = findOperatorIndex(processed, ">");
        if (greaterIndex != -1 && !isPartOfTwoCharOperator(processed, greaterIndex, '>')) {
            String left = processed.substring(0, greaterIndex).trim();
            String right = processed.substring(greaterIndex + 1).trim();

            // Evaluate both sides as math expressions
            double leftValue = evaluateMathExpression(left);
            double rightValue = evaluateMathExpression(right);

            return leftValue > rightValue;
        }

        // Check <
        int lessIndex = findOperatorIndex(processed, "<");
        if (lessIndex != -1 && !isPartOfTwoCharOperator(processed, lessIndex, '<')) {
            String left = processed.substring(0, lessIndex).trim();
            String right = processed.substring(lessIndex + 1).trim();

            // Evaluate both sides as math expressions
            double leftValue = evaluateMathExpression(left);
            double rightValue = evaluateMathExpression(right);

            return leftValue < rightValue;
        }

        // If no operator found or condition is malformed, return false
        return false;
    }

    /**
     * Evaluates a string as a mathematical expression and returns the result as a double.
     * If the string is not a valid math expression, tries to parse it as a number.
     *
     * @param expression The expression to evaluate
     * @return The numeric result
     */
    private static double evaluateMathExpression(@NotNull String expression) {
        try {
            // First, try to use MathExpressionEvaluator if the expression contains operators or functions
            if (containsMathOperators(expression)) {
                return MathExpressionEvaluator.evaluate(expression);
            }

            // If no operators, just parse as a number
            return Double.parseDouble(expression.trim());
        } catch (Exception e) {
            // If evaluation fails, try to parse as a simple number
            try {
                return Double.parseDouble(expression.trim());
            } catch (NumberFormatException nfe) {
                // If that also fails, return 0
                return 0.0;
            }
        }
    }

    /**
     * Evaluates a string as a math expression if needed, otherwise returns it as-is.
     * Used for equality comparisons where we want to preserve string values if they're not numeric.
     *
     * @param expression The expression to evaluate
     * @return The evaluated result as a string
     */
    private static String evaluateMathIfNeeded(@NotNull String expression) {
        if (containsMathOperators(expression)) {
            try {
                double result = MathExpressionEvaluator.evaluate(expression);
                // If the result is a whole number, return it without decimal point
                if (result == Math.floor(result)) {
                    return String.valueOf((long) result);
                }
                return String.valueOf(result);
            } catch (Exception e) {
                // If evaluation fails, return the original expression
                return expression;
            }
        }
        return expression;
    }

    /**
     * Checks if an expression contains mathematical operators or functions.
     */
    private static boolean containsMathOperators(@NotNull String expression) {
        // Check for basic operators (but not comparison operators)
        String trimmed = expression.trim();

        // Check for functions
        if (trimmed.contains("sqrt(") || trimmed.contains("round(") || trimmed.contains("roundDown(")) {
            return true;
        }

        // Check for arithmetic operators
        // We need to be careful not to match negative numbers at the start
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '*' || c == '/') {
                return true;
            }
            // For + and -, check they're not at the start (negative numbers)
            if ((c == '+' || c == '-') && i > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the index of a comparison operator, avoiding operators that are part of math expressions.
     * This looks for comparison operators (==, !=, >=, <=, >, <) that split the condition.
     *
     * @param text The text to search
     * @param operator The operator to find
     * @return The index of the operator, or -1 if not found
     */
    private static int findOperatorIndex(@NotNull String text, @NotNull String operator) {
        // For comparison operators, we want to find them but avoid matching them
        // when they're part of a mathematical expression

        int index = text.indexOf(operator);

        // Make sure we don't match at position 0
        while (index == 0 && text.length() > operator.length()) {
            index = text.indexOf(operator, index + 1);
        }

        return index > 0 ? index : -1;
    }

    /**
     * Checks if a character at the given index is part of a two-character operator.
     */
    private static boolean isPartOfTwoCharOperator(@NotNull String text, int index, char operator) {
        // Check if there's an '=' after this character (making it >= or <=)
        if (index + 1 < text.length() && text.charAt(index + 1) == '=') {
            return true;
        }
        return false;
    }

    // ========================================
    // Component Replacement (Legacy Support)
    // ========================================

    /**
     * Replaces placeholders in an existing item's name and lore.
     */
    public static void replacePlaceholders(@NotNull ItemStack item,
                                           @NotNull Map<String, String> replacements) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Convert string replacements to components
        Map<String, Component> componentReplacements = new HashMap<>();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            componentReplacements.put(entry.getKey(),
                    TextFormatter.transform(null, entry.getValue()));
        }

        // Replace in name
        Component name = meta.itemName();
        if (name != null) {
            meta.itemName(replaceInComponent(name, componentReplacements));
        }

        // Replace in lore
        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            List<Component> newLore = lore.stream()
                    .map(line -> replaceInComponent(line, componentReplacements))
                    .collect(Collectors.toList());
            meta.lore(newLore);
        }

        item.setItemMeta(meta);
    }

    /**
     * Replaces placeholders within a Component using Adventure's API.
     */
    @NotNull
    private static Component replaceInComponent(@NotNull Component component,
                                                @NotNull Map<String, Component> replacements) {
        Component result = component;

        for (Map.Entry<String, Component> entry : replacements.entrySet()) {
            TextReplacementConfig config = TextReplacementConfig.builder()
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
                    .build();
            result = result.replaceText(config);
        }

        return result;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Disables default italic formatting for lore lines.
     */
    @NotNull
    private static Component disableDefaultItalics(@NotNull Component component) {
        return component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET
                ? component.decoration(TextDecoration.ITALIC, false)
                : component;
    }

    /**
     * Gets a list from config, handling both single strings and lists.
     */
    @NotNull
    private static List<String> getConfigList(@NotNull YamlConfiguration config,
                                              @NotNull String path) {
        if (!config.contains(path)) {
            return Collections.emptyList();
        }

        if (config.isList(path)) {
            return config.getStringList(path);
        }

        String value = config.getString(path);
        return value != null ? Collections.singletonList(value) : Collections.emptyList();
    }

    /**
     * Checks if a string is a valid integer.
     */
    private static boolean isInteger(@NotNull String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}