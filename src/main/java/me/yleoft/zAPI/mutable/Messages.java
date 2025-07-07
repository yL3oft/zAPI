package me.yleoft.zAPI.mutable;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.yleoft.zAPI.utils.StringUtils.transform;

/**
 * Messages class to handle messages.
 * This class provides a way to set and get messages with placeholders.
 */
public class Messages {
    public static String COOLDOWN_EXPIRED = "&cYou must wait %timeleft% seconds before using this command again.";

    /**
     * Sets the cooldown expired message.
     * @param message The message to set.
     */
    public static void setCooldownExpired(@NotNull final String message) {
        COOLDOWN_EXPIRED = message;
    }

    /**
     * Gets the cooldown expired message with the time left formatted.
     * @param player The player to whom the message is sent, can be null.
     * @param timeleft The time left in seconds.
     * @return The formatted message.
     */
    @NotNull
    public static String getCooldownExpired(@Nullable final Player player, @NotNull Double timeleft) {
        return transform(player, COOLDOWN_EXPIRED)
                .replace("%timeleft%", String.format("%.2f", timeleft));
    }
    public static String getCooldownExpired(@NotNull Double timeleft) {
        return getCooldownExpired(null, timeleft);
    }

}

