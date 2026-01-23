package me.yleoft.zAPI.hooks;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;

public class HookPlaceholderAPI implements HookInstance {

    public static String message = "PlaceholderAPI has been found, placeholders are enabled!";

    private PlaceholderAPIPlugin plugin;

    @Override
    public boolean exists() {
        return plugin != null;
    }

    @Override
    public void load() {
        if(Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        plugin = PlaceholderAPIPlugin.getInstance();
    }

    @Override
    public String message() {
        return HookPlaceholderAPI.message;
    }

    public static void setMessage(String message) {
        HookPlaceholderAPI.message = message;
    }

}
