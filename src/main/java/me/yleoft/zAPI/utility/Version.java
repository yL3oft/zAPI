package me.yleoft.zAPI.utility;

import com.google.common.primitives.Ints;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version {

    public static final int CURRENT_VERSION = getProtocolVersion();

    public static final int v1_21 = 1210;
    public static final int v1_20 = 1200;
    public static final int v1_19 = 1190;
    public static final int v1_18_1 = 1181;
    public static final int v1_18 = 1180;
    public static final int v1_17 = 1170;
    public static final int v1_16 = 1160;

    public static final boolean HAS_PLAYER_PROFILES = CURRENT_VERSION >= v1_18_1;

    public static boolean USING_FOLIA;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            USING_FOLIA = true;
        } catch (ClassNotFoundException e) {
            USING_FOLIA = false;
        }
    }

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
