package me.yleoft.zAPI.hooks;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.yleoft.zAPI.handlers.PlaceholdersHandler;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HookPlaceholderAPI implements HookInstance {

    private final Map<PlaceholdersHandler, PlaceholderExpansion> expansions = new HashMap<>();
    public static String message = "PlaceholderAPI has been found, placeholders are enabled!";

    private PlaceholderAPIPlugin plugin;

    @Override
    public boolean exists() {
        return plugin != null;
    }

    @Override
    public void load() {
        if(!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        plugin = PlaceholderAPIPlugin.getInstance();
    }

    @Override
    public void unload() {
        if(!exists()) return;
        for(PlaceholdersHandler handler : expansions.keySet()) {
            unregisterPlaceholderExpansion(handler);
        }
    }

    @Override
    public String message() {
        return HookPlaceholderAPI.message;
    }

    public void registerPlaceholderExpansion(PlaceholdersHandler handler) {
        if(HookRegistry.PAPI.exists()) {
            try {
                Object expansion = new PlaceholderExpansion() {
                    @Override
                    public @NotNull String getIdentifier() {
                        return handler.getIdentifier();
                    }

                    @Override
                    public @NotNull String getAuthor() {
                        return handler.getAuthor();
                    }

                    @Override
                    public @NotNull String getVersion() {
                        return handler.getVersion();
                    }

                    @Override
                    public String onRequest(OfflinePlayer p, @NotNull String params) {
                        return handler.applyHookPlaceholders(p, params);
                    }
                };
                ((PlaceholderExpansion) expansion).register();
                expansions.put(handler, (PlaceholderExpansion) expansion);
            }catch (Exception exception) {
                zAPI.getLogger().warn("Failed to register PlaceholderAPI expansion for: " + handler.getIdentifier(), exception);
            }
        }
    }

    public void unregisterPlaceholderExpansion(PlaceholdersHandler handler) {
        if(HookRegistry.PAPI.exists()) {
            PlaceholderExpansion expansion = expansions.remove(handler);
            if (expansion != null) {
                expansion.unregister();
            }
        }
    }

    public static void setMessage(String message) {
        HookPlaceholderAPI.message = message;
    }

}
