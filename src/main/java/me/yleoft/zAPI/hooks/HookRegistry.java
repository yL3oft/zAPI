package me.yleoft.zAPI.hooks;

import me.yleoft.zAPI.zAPI;

import java.util.ArrayList;
import java.util.List;

public class HookRegistry {

    private static final List<HookInstance> Hooks = new ArrayList<>();

    public static final HookPlaceholderAPI PAPI = new HookPlaceholderAPI();

    public static void preload() {
        try {
            Hooks.clear();
            Hooks.add(PAPI);

            Hooks.forEach(instance -> {
                try {
                    instance.preload();
                } catch (Exception exception) {
                    zAPI.getPluginLogger().debug("Failed to preload hook: " + instance.getClass().getSimpleName(), exception);
                }
            });

            Hooks.stream()
                    .filter(HookInstance::exists)
                    .map(HookInstance::preloadMessage)
                    .forEach(zAPI.getPluginLogger()::info);
        } catch (Exception exception) {
            zAPI.getPluginLogger().debug("Unable to initialise hooks.", exception);
        }
    }

    public static void load() {
        try {
            if(Hooks.isEmpty()) {
                Hooks.add(PAPI);
            }

            Hooks.forEach(instance -> {
                try {
                    instance.load();
                } catch (Exception exception) {
                    zAPI.getPluginLogger().debug("Failed to load hook: " + instance.getClass().getSimpleName(), exception);
                }
            });

            Hooks.stream()
                    .filter(HookInstance::exists)
                    .map(HookInstance::message)
                    .forEach(zAPI.getPluginLogger()::info);
        } catch (Exception exception) {
            zAPI.getPluginLogger().debug("Unable to initialise hooks.", exception);
        }
    }

    public static void registerHook(HookInstance hookInstance) {
        Hooks.add(hookInstance);
    }

}
