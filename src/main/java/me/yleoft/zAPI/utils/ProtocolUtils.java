package me.yleoft.zAPI.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for retrieving the protocol version of the server.
 * This class provides methods to get the current protocol version and handle legacy versions.
 */
public class ProtocolUtils {

    /**
     * Retrieves the current protocol version of the server.
     * @return the protocol version or -1 if it fails to retrieve.
     */
    public static int getProtocolVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.contains("v") ? packageName.substring(packageName.lastIndexOf('.') + 1) : "";
            Class<?> sharedConstantsClass = getNMSClass("SharedConstants", version);
            if (sharedConstantsClass == null) return -1;
            Object mcVersion = sharedConstantsClass.getMethod("getCurrentVersion").invoke(null);
            Method getProtocolMethod = mcVersion.getClass().getMethod("getProtocolVersion");
            return (int) getProtocolMethod.invoke(mcVersion);
        } catch (Throwable t) {
            return tryLegacyProtocolVersion();
        }
    }

    /**
     * Attempts to retrieve the protocol version using legacy methods.
     * This is a fallback for older versions of Minecraft where the new method may not exist.
     * @return the protocol version or -1 if it fails.
     */
    private static int tryLegacyProtocolVersion() {
        try {
            Class<?> serverClass = getLegacyNMSClass("MinecraftServer");
            Method getServerMethod = serverClass.getMethod("getServer");
            Object server = getServerMethod.invoke(null);

            Field pingField = server.getClass().getDeclaredField("K");
            pingField.setAccessible(true);
            return pingField.getInt(server);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Get an NMS class by name.
     * @param name the name of the class to retrieve.
     * @return the class
     */
    @Nullable
    private static Class<?> getNMSClass(@NotNull final String name, String version) {
        try {
            return Class.forName("net.minecraft." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get a legacy NMS class by name.
     * @param name the name of the class to retrieve.
     * @return the class
     */
    @Nullable
    private static Class<?> getLegacyNMSClass(@NotNull String name) {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.contains("v") ? packageName.substring(packageName.lastIndexOf('.') + 1) : "";
            String className = version.isEmpty()
                    ? "net.minecraft.server." + name
                    : "net.minecraft.server." + version + "." + name;
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
