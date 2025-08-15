package me.yleoft.zAPI.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for retrieving the protocol version of the server.
 * This class provides methods to get the current protocol version and handle legacy versions.
 */
public abstract class ProtocolUtils {

    /**
     * Retrieves the current protocol version of the server.
     * @return the protocol version or -1 if it fails to retrieve.
     */
    public static int getProtocolVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.contains("v") ? packageName.substring(packageName.lastIndexOf('.') + 1) : "";
            Class<?> sharedConstantsClass = getNMSClass();
            if (sharedConstantsClass == null) {
                System.out.println("[ProtocolUtils] SharedConstants class not found for version " + version);
                return -1;
            }
            Object mcVersion = sharedConstantsClass.getMethod("getCurrentVersion").invoke(null);
            Method method;
            try {
                method = mcVersion.getClass().getMethod("protocolVersion");
            } catch (NoSuchMethodException e) {
                method = mcVersion.getClass().getMethod("getProtocolVersion");
            }
            return (int) method.invoke(mcVersion);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("[ProtocolUtils] Falling back to legacy protocol version.");
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
            Class<?> serverClass = getLegacyNMSClass();
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
     *
     * @return the class
     */
    @Nullable
    private static Class<?> getNMSClass() {
        try {
            return Class.forName("net.minecraft." + "SharedConstants");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get a legacy NMS class by name.
     *
     * @return the class
     */
    @Nullable
    private static Class<?> getLegacyNMSClass() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.contains("v") ? packageName.substring(packageName.lastIndexOf('.') + 1) : "";
            String className = version.isEmpty()
                    ? "net.minecraft.server." + "MinecraftServer"
                    : "net.minecraft.server." + version + "." + "MinecraftServer";
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
