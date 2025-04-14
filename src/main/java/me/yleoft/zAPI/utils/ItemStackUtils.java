package me.yleoft.zAPI.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static me.yleoft.zAPI.utils.ConfigUtils.formPath;
import static me.yleoft.zAPI.utils.NbtUtils.addCustomCommands;
import static me.yleoft.zAPI.zAPI.stringUtils;

public abstract class ItemStackUtils {

    /**
     * Creates an ItemStack from a YamlConfiguration file.
     * @param player The player to use for string transformation.
     * @param config The {@link YamlConfiguration} to load the item from.
     * @param path The path to the item in the config file.
     * @return The created ItemStack.
     */
    public static ItemStack getItemFromConfig(@Nullable Player player, @NotNull YamlConfiguration config, @Nullable String path, @Nullable HashMap<String, String> replacers) {
        if (path == null) path = "";
        String materialPath = formPath(path, "material");
        String amountPath = formPath(path, "amount");
        String namePath = formPath(path, "name");
        String lorePath = formPath(path, "lore");
        String commandsPath = formPath(path, "commands");
        int amount = config.contains(amountPath) ? config.getInt(amountPath) : 1;
        ItemStack item;
        if(config.isList(materialPath)) {
            List<String> materials = config.getStringList(materialPath);
            item = ItemStackUtils.getItem(materials.get((int) (Math.floor(Math.random() * materials.size()))), amount);
        }else {
            item = ItemStackUtils.getItem(requireNonNull(config.getString(materialPath)), amount);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(namePath))
                meta.setDisplayName(stringUtils.transform(player, requireNonNull(config.getString(namePath))));
            if (config.contains(lorePath)) {
                List<String> lore;
                if (config.isList(lorePath)) {
                    lore = config.getStringList(lorePath);
                    List<String> transformedLore = new ArrayList<>();
                    lore.forEach(loreLine -> transformedLore.add(stringUtils.transform(player, loreLine)));
                    lore = transformedLore;
                } else {
                    lore = List.of(stringUtils.transform(player, requireNonNull(config.getString(lorePath))));
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
    public static ItemStack getItemFromConfig(@Nullable Player player, @NotNull YamlConfiguration config, @Nullable String path) {
        return getItemFromConfig(player, config, path, null);
    }

    public static ItemStack getItem(@NotNull String materialString, int amount) {
        Material material = MaterialUtils.getMaterial(materialString);
        return new ItemStack(material, amount);
    }

    /**
     * Replaces all the lore and name of the item with the given replaces
     * @param item The {@link ItemStack} to replace the lore and name of
     * @param replaces The {@link HashMap} of replaces to do
     */
    public static void replaceAll(@NotNull ItemStack item, @NotNull HashMap<String, String> replaces) {
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
    public static ItemMeta replaceLore(@NotNull ItemMeta meta, @NotNull HashMap<String, String> replaces) {
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
    public static ItemStack replaceLore(@NotNull ItemStack item, @NotNull HashMap<String, String> replaces) {
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
    public static ItemMeta replaceName(@NotNull ItemMeta meta, @NotNull HashMap<String, String> replaces) {
        String name = meta.getDisplayName();
        for (String key : replaces.keySet()) {
            name = name.replace(key, replaces.get(key));
        }
        meta.setDisplayName(name);
        return meta;
    }
    public static ItemStack replaceName(@NotNull ItemStack item, @NotNull HashMap<String, String> replaces) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            replaceName(meta, replaces);
            item.setItemMeta(meta);
        }
        return item;
    }

}
