package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static me.yleoft.zAPI.utils.ConfigUtils.formPath;
import static me.yleoft.zAPI.utils.HeadUtils.getPlayerHeadFromString;
import static me.yleoft.zAPI.utils.MaterialUtils.isLegacyMaterial;
import static me.yleoft.zAPI.utils.NbtUtils.addCustomCommands;
import static me.yleoft.zAPI.utils.StringUtils.applyPlaceholders;

public abstract class ItemStackUtils {

    public static final HashMap<String, Integer> legacyColors = new HashMap<>();

    static {
        legacyColors.put("WHITE", 0);
        legacyColors.put("ORANGE", 1);
        legacyColors.put("MAGENTA", 2);
        legacyColors.put("LIGHT_BLUE", 3);
        legacyColors.put("YELLOW", 4);
        legacyColors.put("LIME", 5);
        legacyColors.put("PINK", 6);
        legacyColors.put("GRAY", 7);
        legacyColors.put("LIGHT_GRAY", 8);
        legacyColors.put("CYAN", 9);
        legacyColors.put("PURPLE", 10);
        legacyColors.put("BLUE", 11);
        legacyColors.put("BROWN", 12);
        legacyColors.put("GREEN", 13);
        legacyColors.put("RED", 14);
        legacyColors.put("BLACK", 15);
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
        boolean head = false;
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
            if (config.contains(namePath))
                meta.setDisplayName(StringUtils.transform(player, requireNonNull(config.getString(namePath))));
            if (config.contains(lorePath)) {
                List<String> lore;
                if (config.isList(lorePath)) {
                    lore = config.getStringList(lorePath);
                    List<String> transformedLore = new ArrayList<>();
                    lore.forEach(loreLine -> transformedLore.add(StringUtils.transform(player, loreLine)));
                    lore = transformedLore;
                } else {
                    lore = List.of(StringUtils.transform(player, requireNonNull(config.getString(lorePath))));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        List<String> commands = new ArrayList<>();
        if (config.contains(commandsPath)) {
            if (config.isList(commandsPath)) {
                commands = config.getStringList(commandsPath);
            } else {
                commands = List.of(requireNonNull(config.getString(commandsPath)));
            }
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
    public static @NotNull ItemMeta replaceLore(@NotNull ItemMeta meta, @NotNull final HashMap<String, String> replaces) {
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
        short data = 0;
        color = color.toUpperCase();
        material = material.toUpperCase();
        if (legacyColors.containsKey(color)) {
            data = (short) legacyColors.get(color).intValue();
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

        if (legacyColors.keySet().stream().anyMatch(modernName::startsWith)) {
            String[] split = modernName.split("_");
            String color = requireNonNull(legacyColors.keySet().stream()
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

}
