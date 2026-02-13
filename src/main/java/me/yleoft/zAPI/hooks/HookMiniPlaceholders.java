package me.yleoft.zAPI.hooks;

import org.bukkit.Bukkit;

public class HookMiniPlaceholders implements HookInstance {

    public static String message = "MiniPlaceholders has been found, mini placeholders are enabled!";

    private boolean exists = false;

    @Override
    public boolean exists() {
        return exists;
    }

    @Override
    public void load() {
        if(!Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) return;
        exists = true;
    }

    @Override
    public String message() {
        return message;
    }

    public static void setMessage(String message) {
        HookMiniPlaceholders.message = message;
    }

}
