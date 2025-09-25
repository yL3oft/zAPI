package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static me.yleoft.zAPI.utils.ConfigUtils.formPath;
import static me.yleoft.zAPI.utils.HeadUtils.getPlayerHeadFromString;
import static me.yleoft.zAPI.utils.MaterialUtils.isLegacyMaterial;
import static me.yleoft.zAPI.utils.NbtUtils.addCustomCommands;
import static me.yleoft.zAPI.utils.StringUtils.applyPlaceholders;

public abstract class ItemStackUtils {

    /**
     * The mark used to identify items in the inventory.
     */
    public static final String mark = "zAPI:unpickable";
    public static final Map<String, Integer> LEGACY_COLORS;

    static {
        Map<String, Integer> _map = new HashMap<>();
        _map.put("WHITE", 0);
        _map.put("ORANGE", 1);
        _map.put("MAGENTA", 2);
        _map.put("LIGHT_BLUE", 3);
        _map.put("YELLOW", 4);
        _map.put("LIME", 5);
        _map.put("PINK", 6);
        _map.put("GRAY", 7);
        _map.put("LIGHT_GRAY", 8);
        _map.put("CYAN", 9);
        _map.put("PURPLE", 10);
        _map.put("BLUE", 11);
        _map.put("BROWN", 12);
        _map.put("GREEN", 13);
        _map.put("RED", 14);
        _map.put("BLACK", 15);
        LEGACY_COLORS = Collections.unmodifiableMap(_map);
    }

    /**
     * Creates an ItemStack from a YamlConfiguration file.
     * @param player The player to use for string transformation.
     * @param config The {@link YamlConfiguration} to load the item from.
     * @param path The path to the item in the config file.
     * @return The created ItemStack.
     */
    public static @NotNull ItemStack getItemFromConfig(@Nullable final Player player, @NotNull final YamlConfiguration config, @Nullable String path, @Nullable final HashMap<String, String> replacers) {
        if (path == null) path = "";
        String materialPath = formPath(path, "material");
        String amountPath = formPath(path, "amount");
        String namePath = formPath(path, "name");
        String lorePath = formPath(path, "lore");
        String enchantmentsPath = formPath(path, "enchantments");
        String unbreakablePath = formPath(path, "unbreakable");
        String itemflagsPath = formPath(path, "itemflags");
        String pickablePath = formPath(path, "pickable");
        String commandsPath = formPath(path, "commands");
        int amount = config.contains(amountPath) ? config.getInt(amountPath) : 1;
        ItemStack item;
        String material;
        if(config.isList(materialPath)) {
            List<String> materials = config.getStringList(materialPath);
            material = materials.get((int) (Math.floor(Math.random() * materials.size())));
        }else {
            material = requireNonNull(config.getString(materialPath));
        }
        if(material.startsWith("head-") || material.startsWith("base64head-")) {
            String[] split = material.split("-");
            String type = split[0];
            String value = applyPlaceholders(player, split[1]);
            item = getPlayerHeadFromString(type, value);
        }else if(isLegacyMaterial(material)) {
            item = getLegacyItem(material, amount);
        } else item = getItem(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(namePath) && config.isString(namePath)) {
                meta.setDisplayName(StringUtils.transform(player, requireNonNull(config.getString(namePath))));
            }
            if (config.contains(lorePath) && (config.isString(namePath) || config.isList(lorePath))) {
                List<String> lore;
                lore = config.isList(lorePath)
                        ? config.getStringList(lorePath).stream()
                        .map(loreLine -> StringUtils.transform(player, loreLine))
                        .collect(Collectors.toList())
                        : Collections.singletonList(StringUtils.transform(player, requireNonNull(config.getString(lorePath))));
                meta.setLore(lore);
            }
            if(config.contains(enchantmentsPath) && (config.isString(namePath) || config.isList(lorePath))) {
                List<String> enchantments = config.isList(enchantmentsPath)
                        ? config.getStringList(enchantmentsPath)
                        : Collections.singletonList(requireNonNull(config.getString(enchantmentsPath)));
                for (String enchantmentString : enchantments) {
                    enchantmentString = applyPlaceholders(player, enchantmentString);
                    int level = 1;
                    if(enchantmentString.contains(";")) {
                        String[] split = enchantmentString.split(";");
                        if(split.length == 2 && StringUtils.isInteger(split[1])) level = Integer.parseInt(split[1]) > 1 ? Integer.parseInt(split[1]) : level;
                        enchantmentString = split[0];
                    }
                    Enchantment enchantment = EnchantmentUtils.getEnchantment(enchantmentString);
                    if(enchantment != null) meta.addEnchant(enchantment, level, true);
                }
            }
            if(config.contains(unbreakablePath) && config.isBoolean(unbreakablePath)) {
                boolean unbreakable = config.getBoolean(unbreakablePath);
                meta.setUnbreakable(unbreakable);
            }
            if(config.contains(itemflagsPath) && (config.isString(namePath) || config.isList(lorePath))) {
                List<String> itemFlags = config.isList(itemflagsPath)
                        ? config.getStringList(itemflagsPath)
                        : Collections.singletonList(requireNonNull(config.getString(itemflagsPath)));
                List<ItemFlag> flagsToAdd = new ArrayList<>();
                for (String flagString : itemFlags) {
                    if (flagString == null || flagString.trim().isEmpty()) continue;
                    flagString = applyPlaceholders(player, flagString).trim();
                    ItemFlag flag = getItemFlagFromString(flagString);
                    if (flag != null) flagsToAdd.add(flag);
                }
                if (!flagsToAdd.isEmpty()) {
                    meta.addItemFlags(flagsToAdd.toArray(new ItemFlag[0]));
                }
            }
            item.setItemMeta(meta);
        }
        boolean pickable = config.contains(pickablePath) && config.isBoolean(pickablePath) && config.getBoolean(pickablePath);
        if(!pickable) NbtUtils.markItem(item, mark);
        List<String> commands = new ArrayList<>();
        if (config.contains(commandsPath)) {
            commands = config.isList(commandsPath)
                    ? config.getStringList(commandsPath)
                    : Collections.singletonList(requireNonNull(config.getString(commandsPath)));
        }
        if (replacers != null && !replacers.isEmpty()) {
            addCustomCommands(item, commands, replacers);
        }else {
            addCustomCommands(item, commands);
        }
        return item;
    }
    public static @NotNull ItemStack getItemFromConfig(@Nullable final Player player, @NotNull final YamlConfiguration config, @Nullable String path) {
        return getItemFromConfig(player, config, path, null);
    }

    public static @NotNull ItemStack getItem(@NotNull final String materialString, int amount) {
        Material material = MaterialUtils.getMaterial(materialString);
        return new ItemStack(material, amount);
    }

    /**
     * Replaces all the lore and name of the item with the given replaces
     * @param item The {@link ItemStack} to replace the lore and name of
     * @param replaces The {@link HashMap} of replaces to do
     */
    public static void replaceAll(@NotNull ItemStack item, @NotNull final HashMap<String, String> replaces) {
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            replaceName(meta, replaces);
            replaceLore(meta, replaces);
            item.setItemMeta(meta);
        }
    }

    /**
     * Replaces the lore of the item with the given replaces
     * @param meta The {@link ItemMeta} to replace the lore of
     * @param replaces The {@link HashMap} of replaces to do
     * @return The {@link ItemMeta} with the replaced lore
     */
    public static ItemMeta replaceLore(@NotNull ItemMeta meta, @NotNull final HashMap<String, String> replaces) {
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                for (String key : replaces.keySet()) {
                    line = line.replace(key, replaces.get(key));
                }
                lore.set(i, line);
            }
            meta.setLore(lore);
        }
        return meta;
    }

    /**
     * Replaces the lore of the item with the given replaces
     * @param item The {@link ItemStack} to replace the lore of
     * @param replaces The {@link HashMap} of replaces to do
     * @return The {@link ItemMeta} with the replaced lore
     */
    public static ItemStack replaceLore(@NotNull ItemStack item, @NotNull final HashMap<String, String> replaces) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    for (String key : replaces.keySet()) {
                        line = line.replace(key, replaces.get(key));
                    }
                    lore.set(i, line);
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Replaces the name of the item with the given replaces
     * @param meta The {@link ItemMeta} to replace the name of
     * @param replaces The {@link HashMap} of replaces to do
     * @return The {@link ItemMeta} with the replaced name
     */
    public static @NotNull ItemMeta replaceName(@NotNull ItemMeta meta, @NotNull final HashMap<String, String> replaces) {
        String name = meta.getDisplayName();
        for (String key : replaces.keySet()) {
            name = name.replace(key, replaces.get(key));
        }
        meta.setDisplayName(name);
        return meta;
    }
    public static @NotNull ItemStack replaceName(@NotNull ItemStack item, @NotNull final HashMap<String, String> replaces) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            replaceName(meta, replaces);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a legacy item from the given color and material.
     * @param color The color of the item.
     * @param material The material of the item.
     * @return The created ItemStack.
     */
    public static @NotNull ItemStack getLegacyItem(@NotNull String color, @NotNull String material) {
        short data;
        color = color.toUpperCase();
        material = material.toUpperCase();
        if (LEGACY_COLORS.containsKey(color)) {
            data = (short) LEGACY_COLORS.get(color).intValue();
        } else {
            throw new IllegalArgumentException("Invalid color: " + color);
        }
        try {
            ItemStack item = new ItemStack(requireNonNull(Material.getMaterial(material)), 1);
            item.setDurability(data);
            return item;
        }catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid material: " + material, e);
        }
    }
    public static @NotNull ItemStack getLegacyItem(@NotNull final String modernName, int amount) {
        Material material = Material.STONE;
        short data = 0;

        if (LEGACY_COLORS.keySet().stream().anyMatch(modernName::startsWith)) {
            String color = requireNonNull(LEGACY_COLORS.keySet().stream()
                    .filter(modernName::startsWith)
                    .findFirst()
                    .orElse(null));
            String type = modernName.substring(color.length()+1);
            ItemStack item = getLegacyItem(color, type);
            item.setAmount(amount);
            return item;
        }

        ItemStack item = new ItemStack(material, amount);
        item.setDurability(data);
        return item;
    }

    /**
     * Tries to get an ItemFlag from a string.
     * @param raw The raw string to parse.
     * @return The ItemFlag, or null if not found.
     */
    private static @Nullable ItemFlag getItemFlagFromString(@NotNull String raw) {
        String s = raw.trim();
        s = s.replace(" ", "_").replace("-", "_");
        try {
            return ItemFlag.valueOf(s.toUpperCase());
        } catch (Exception ignored) {}

        String upper = s.toUpperCase();
        String withoutHide = upper.startsWith("HIDE_") ? upper.substring(5) : upper;
        String lower = withoutHide.toLowerCase();

        if (lower.contains("enchant")) {
            return ItemFlag.HIDE_ENCHANTS;
        }
        if (lower.contains("attribut") || lower.contains("attributte") || lower.contains("attributtes") || lower.contains("attributes")) {
            return ItemFlag.HIDE_ATTRIBUTES;
        }
        if (lower.contains("unbreak")) {
            return ItemFlag.HIDE_UNBREAKABLE;
        }
        if (lower.contains("destro")) {
            return ItemFlag.HIDE_DESTROYS;
        }
        if (lower.contains("placed") || lower.contains("placed_on")) {
            return ItemFlag.HIDE_PLACED_ON;
        }

        try {
            return ItemFlag.valueOf("HIDE_" + withoutHide);
        } catch (Exception ignored) {}

        return null;
    }

}
