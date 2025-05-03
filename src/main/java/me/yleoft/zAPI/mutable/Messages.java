package me.yleoft.zAPI.mutable;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.yleoft.zAPI.utils.StringUtils.transform;

public class Messages {
    public static String COOLDOWN_EXPIRED = "&cYou must wait %timeleft% seconds before using this command again.";

    public static void setCooldownExpired(@NotNull String message) {
        COOLDOWN_EXPIRED = message;
    }

    public static String getCooldownExpired(@Nullable Player player, @NotNull Double timeleft) {
        return transform(player, COOLDOWN_EXPIRED)
                .replace("%timeleft%", String.format("%.2f", timeleft));
    }
    public static String getCooldownExpired(@NotNull Double timeleft) {
        return getCooldownExpired(null, timeleft);
    }

}

