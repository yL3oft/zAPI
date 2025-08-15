package me.yleoft.zAPI.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * Utility class for sending action bar messages to players.
 * This class handles different Minecraft versions and uses reflection to send messages.
 */
public abstract class ActionbarUtils extends ProtocolUtils {

    private static final String version;
    private static final boolean legacy = getProtocolVersion() <= 47;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = packageName.split("\\.");
        version = parts.length > 3 ? parts[3] : "";
    }

    /**
     * Sends an action bar message to a player.
     *
     * @param player  The player to send the message to.
     * @param message The message to send.
     */
    public static void send(@NotNull Player player, @NotNull String message) {
        if (!legacy) {
            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                return;
            } catch (Exception ignored) {}
        }

        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);

            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Method a = chatSerializer.getMethod("a", String.class);
            Object icbc = a.invoke(null, "{\"text\":\"" + message + "\"}");

            Class<?> packetPlayOutChat = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor<?> constructor = packetPlayOutChat.getConstructor(
                    Class.forName("net.minecraft.server." + version + ".IChatBaseComponent"), byte.class
            );

            Object packet = constructor.newInstance(icbc, (byte) 2);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet")).invoke(connection, packet);
        } catch (Exception ignored) {
        }
    }
}
