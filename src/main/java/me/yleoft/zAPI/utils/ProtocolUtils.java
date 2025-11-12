package me.yleoft.zAPI.utils;

import com.google.common.primitives.Ints;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for retrieving the protocol version of the server.
 * This class provides methods to get the current protocol version and handle legacy versions.
 */
public abstract class ProtocolUtils {

    public static final int CURRENT_VERSION = getProtocolVersion();

    public static final int v1_21 = 1210;
    public static final int v1_20 = 1200;
    public static final int v1_19 = 1190;
    public static final int v1_18_1 = 1181;
    public static final int v1_18 = 1180;
    public static final int v1_17 = 1170;
    public static final int v1_16 = 1160;
    public static final int v1_15 = 1150;
    public static final int v1_14 = 1140;
    public static final int v1_13 = 1130;
    public static final int v1_12 = 1120;

    public static final boolean HAS_PLAYER_PROFILES = CURRENT_VERSION >= v1_18_1;
    public static final boolean IS_SKULL_OWNER_LEGACY = CURRENT_VERSION <= v1_12;

    /**
     * Retrieves the current protocol version of the server.
     *
     * @return The protocol version as an integer.
     * @throws RuntimeException if the protocol version cannot be determined.
     */
    private static int getProtocolVersion() {
        final Matcher matcher = Pattern.compile("(?<version>\\d+\\.\\d+)(?<patch>\\.\\d+)?").matcher(Bukkit.getBukkitVersion());
        final StringBuilder stringBuilder = new StringBuilder();
        if (matcher.find()) {
            stringBuilder.append(matcher.group("version").replace(".", ""));
            final String patch = matcher.group("patch");
            if (patch == null) stringBuilder.append("0");
            else stringBuilder.append(patch.replace(".", ""));
        }
        final Integer version = Ints.tryParse(stringBuilder.toString());
        if (version == null) throw new RuntimeException("Could not retrieve server protocol version!");
        return version;
    }

}
