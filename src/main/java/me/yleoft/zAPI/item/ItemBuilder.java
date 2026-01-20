package me.yleoft.zAPI.item;

import me.yleoft.zAPI.skull.HeadProvider;
import me.yleoft.zAPI.utility.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
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
import static me.yleoft.zAPI.configuration.Path.formPath;
import static me.yleoft.zAPI.item.NbtHandler.mark;

public abstract class ItemBuilder {

    /**
     * Creates an ItemStack from a YamlConfiguration file.
     * @param player The player to use for string transformation.
     * @param config The {@link YamlConfiguration} to load the item from.
     * @param path The path to the item in the config file.
     * @return The created ItemStack.
     */
    public static @NotNull ItemStack getItemFromConfig(@Nullable final Player player, @NotNull final YamlConfiguration config, @Nullable String path, @Nullable final Map<String, Component> replacers) {
        if (path == null) path = "";

        String materialPath = formPath(path, "material");
        String material = config.isList(materialPath)
                ? getRandomElement(config.getStringList(materialPath))
                : config.getString(materialPath, "STONE");

        int amount = config.getInt(formPath(path, "amount"), 1);
        ItemStack item = createItemFromMaterial(player, material, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyName(meta, config, formPath(path, "name"), player);
            applyLore(meta, config, formPath(path, "lore"), player);
            applyEnchantments(meta, config, formPath(path, "enchantments"), player);
            applyUnbreakable(meta, config, formPath(path, "unbreakable"));
            applyItemFlags(meta, config, formPath(path, "itemflags"), player);
            item.setItemMeta(meta);
        }

        boolean pickable = config.getBoolean(formPath(path, "pickable"), false);
        NbtHandler.markItem(item, mark, !pickable);

        List<String> commands = getConfigList(config, formPath(path, "commands"));
        if (replacers != null && !replacers.isEmpty()) {
            Map<String, String> stringReplacers = new HashMap<>();
            for (Map.Entry<String, Component> entry : replacers.entrySet()) {
                stringReplacers.put(entry.getKey(), net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entry.getValue()));
            }
            NbtHandler.addCustomCommands(item, commands, stringReplacers);
        } else {
            NbtHandler.addCustomCommands(item, commands);
        }

        return item;
    }

    public static @NotNull ItemStack getItemFromConfigString(@Nullable final Player player, @NotNull final YamlConfiguration config, @Nullable String path, @Nullable final Map<String, String> replacers) {
        Map<String, Component> componentReplacers = null;
        if (replacers != null) {
            componentReplacers = new HashMap<>();
            for (Map.Entry<String, String> entry :  replacers.entrySet()) {
                componentReplacers. put(entry.getKey(), Component.text(entry.getValue()));
            }
        }
        return getItemFromConfig(player, config, path, componentReplacers);
    }

    public static @NotNull ItemStack getItemFromConfig(@Nullable final Player player, @NotNull final YamlConfiguration config, @Nullable String path) {
        return getItemFromConfig(player, config, path, null);
    }

    public static @NotNull ItemStack getItem(@NotNull final String materialString, int amount) {
        Material material = getMaterial(materialString);
        return new ItemStack(material, amount);
    }

    public static @NotNull Material getMaterial(@NotNull final String materialString) {
        if(TextFormatter.startsWithIgnoreCase(materialString, "[head]")) {
            return Material.PLAYER_HEAD;
        }
        return Objects.requireNonNull(Material.getMaterial(materialString));
    }

    /**
     * Replaces all the lore and name of the item with the given replaces
     * @param item The {@link ItemStack} to replace the lore and name of
     * @param replaces The {@link Map} of replaces to do
     */
    public static void replaceAll(@NotNull ItemStack item, @NotNull final Map<String, Component> replaces) {
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            replaceName(meta, replaces);
            replaceLore(meta, replaces);
            item.setItemMeta(meta);
        }
    }
    public static void replaceAllString(@NotNull ItemStack item, @NotNull final Map<String, String> replaces) {
        Map<String, Component> componentReplaces = new HashMap<>();
        for (Map.Entry<String, String> entry : replaces.entrySet()) {
            componentReplaces.put(entry.getKey(), TextFormatter.transform(null, entry.getValue()));
        }
        replaceAll(item, componentReplaces);
    }

    /**
     * Replaces the lore of the item with the given replaces
     * @param meta The {@link ItemMeta} to replace the lore of
     * @param replaces The {@link Map} of replaces to do
     */
    public static void replaceLore(@NotNull ItemMeta meta, @NotNull final Map<String, Component> replaces) {
        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            List<Component> newLore = lore.stream()
                    .map(component -> replaceInComponent(component, replaces))
                    .collect(Collectors.toList());
            meta.lore(newLore);
        }
    }

    /**
     * Replaces the lore of the item with the given replaces
     * @param item The {@link ItemStack} to replace the lore of
     * @param replaces The {@link Map} of replaces to do
     * @return The {@link ItemStack} with the replaced lore
     */
    public static ItemStack replaceLore(@NotNull ItemStack item, @NotNull final Map<String, Component> replaces) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            replaceLore(meta, replaces);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Replaces the name of the item with the given replaces
     * @param meta The {@link ItemMeta} to replace the name of
     * @param replaces The {@link Map} of replaces to do
     */
    public static void replaceName(@NotNull ItemMeta meta, @NotNull final Map<String, Component> replaces) {
        Component name = meta.itemName();
        if (name != null) {
            Component newName = replaceInComponent(name, replaces);
            meta.itemName(newName);
        }
    }

    public static @NotNull ItemStack replaceName(@NotNull ItemStack item, @NotNull final Map<String, Component> replaces) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            replaceName(meta, replaces);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Replaces placeholders in a Component using Adventure's native replacement API.
     *
     * @param component The {@link Component} to perform replacements on
     * @param replaces The {@link Map} of replaces to do
     * @return The {@link Component} with replacements applied
     */
    private static Component replaceInComponent(@NotNull Component component, @NotNull final Map<String, Component> replaces) {
        Component result = component;

        for (Map.Entry<String, Component> entry : replaces.entrySet()) {
            TextReplacementConfig config = TextReplacementConfig.builder()
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
                    .build();
            result = result.replaceText(config);
        }

        return result;
    }

    public static ItemStack createItemFromMaterial(@Nullable Player player, @NotNull String material, int amount) {
        if (material.startsWith("head-") || material.startsWith("base64head-") ||
                material.startsWith("namehead-") || material.startsWith("urlhead-")) {
            String[] split = material.split("-", 2);
            String value = TextFormatter.applyPlaceholders(player, split[1]);
            return HeadProvider.getPlayerHeadFromString(split[0], value);
        }
        return getItem(material, amount);
    }

    public static @NotNull String getRandomElement(@NotNull List<String> list) {
        return list.isEmpty() ? "STONE" : list.get((int) (Math.random() * list.size()));
    }

    public static @Nullable ItemFlag getItemFlagFromString(@NotNull String raw) {
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

    private static @NotNull List<String> getConfigList(@NotNull YamlConfiguration config, @NotNull String path) {
        if (!config.contains(path)) return Collections.emptyList();
        return config.isList(path)
                ? config.getStringList(path)
                : Collections.singletonList(config.getString(path, ""));
    }

    private static void applyName(@NotNull ItemMeta meta, @NotNull YamlConfiguration config, @NotNull String path, @Nullable Player player) {
        if (config.isString(path)) {
            meta.itemName(TextFormatter.transform(player, requireNonNull(config.getString(path))));
        }
    }

    private static void applyLore(@NotNull ItemMeta meta, @NotNull YamlConfiguration config, @NotNull String path, @Nullable Player player) {
        if (!config.contains(path)) return;

        List<Component> lore = config.isList(path)
                ? config.getStringList(path).stream()
                .map(line -> disableDefaultLoreItalics(TextFormatter.transform(player, line)))
                .collect(Collectors.toList())
                : Collections.singletonList(
                disableDefaultLoreItalics(TextFormatter.transform(player, requireNonNull(config.getString(path))))
        );

        meta.lore(lore);
    }

    /**
     * Minecraft renders item lore italic by default when the Component doesn't explicitly set italics.
     * We disable italics ONLY when it's NOT_SET, so explicit <italic:true> in MiniMessage still works.
     */
    private static @NotNull Component disableDefaultLoreItalics(@NotNull Component component) {
        return component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET
                ? component.decoration(TextDecoration.ITALIC, false)
                : component;
    }

    private static void applyEnchantments(@NotNull ItemMeta meta, @NotNull YamlConfiguration config, @NotNull String path, @Nullable Player player) {
        List<String> enchantments = getConfigList(config, path);

        for (String enchStr : enchantments) {
            enchStr = TextFormatter.applyPlaceholders(player, enchStr);
            String[] split = enchStr.split(";");
            int level = split.length == 2 && TextFormatter.isInteger(split[1]) ? Integer.parseInt(split[1]) : 1;

            Enchantment enchantment = Enchantment.getByName(split[0]);
            if (enchantment != null) {
                meta.addEnchant(enchantment, Math.max(1, level), true);
            }
        }
    }

    private static void applyUnbreakable(@NotNull ItemMeta meta, @NotNull YamlConfiguration config, @NotNull String path) {
        if (config.isBoolean(path)) {
            meta.setUnbreakable(config.getBoolean(path));
        }
    }

    private static void applyItemFlags(@NotNull ItemMeta meta, @NotNull YamlConfiguration config, @NotNull String path, @Nullable Player player) {
        List<String> flagStrings = getConfigList(config, path);

        List<ItemFlag> flags = flagStrings.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> TextFormatter.applyPlaceholders(player, s).trim())
                .map(ItemBuilder::getItemFlagFromString)
                .filter(Objects::nonNull)
                .toList();

        if (!flags.isEmpty()) {
            meta.addItemFlags(flags.toArray(new ItemFlag[0]));
        }
    }

}