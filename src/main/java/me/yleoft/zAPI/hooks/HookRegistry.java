package me.yleoft.zAPI.hooks;

import me.yleoft.zAPI.zAPI;

import java.util.ArrayList;
import java.util.List;

public class HookRegistry {

    private static final List<HookInstance> Hooks = new ArrayList<>();

    public static final HookPlaceholderAPI PAPI = new HookPlaceholderAPI();

    public static void preload() {
        try {
            boolean debugMode = zAPI.getPluginLogger().isDebugMode();
            zAPI.getPluginLogger().setDebugMode(true);
            if(!Hooks.contains(PAPI)) {
                Hooks.add(PAPI);
            }

            Hooks.forEach(instance -> {
                try {
                    instance.preload();
                } catch (NoClassDefFoundError | Exception exception) {
                    zAPI.getPluginLogger().error("Failed to preload hook: " + instance.getClass().getSimpleName(), exception);
                }
            });

            Hooks.stream()
                    .filter(HookInstance::exists)
                    .map(HookInstance::preloadMessage)
                    .forEach(zAPI.getPluginLogger()::info);
            zAPI.getPluginLogger().setDebugMode(debugMode);
        } catch (NoClassDefFoundError | Exception exception) {
            zAPI.getPluginLogger().error("Unable to pre load hooks.", exception);
        }
    }

    public static void load() {
        try {
            if(!Hooks.contains(PAPI)) {
                Hooks.add(PAPI);
            }

            Hooks.forEach(instance -> {
                try {
                    instance.load();
                } catch (NoClassDefFoundError | Exception exception) {
                    zAPI.getPluginLogger().error("Failed to load hook: " + instance.getClass().getSimpleName(), exception);
                }
            });

            Hooks.stream()
                    .filter(HookInstance::exists)
                    .map(HookInstance::message)
                    .forEach(zAPI.getPluginLogger()::info);
        } catch (Exception exception) {
            zAPI.getPluginLogger().error("Unable to initialise hooks.", exception);
        }
    }

    public static void unload() {
        Hooks.forEach(instance -> {
            try {
                instance.unload();
            } catch (Exception exception) {
                zAPI.getPluginLogger().error("Failed to unload hook: " + instance.getClass().getSimpleName(), exception);
            }
        });
    }

    public static void registerHook(HookInstance hookInstance) {
        Hooks.add(hookInstance);
    }

    public static void clearHooks() {
        Hooks.clear();
    }

}
