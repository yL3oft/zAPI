package me.yleoft.zAPI.utility;

import com.google.common.primitives.Ints;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling server version and compatibility checks.
 */
public final class Version {

    public static final int CURRENT_VERSION = getProtocolVersion();

    public static final int v1_21 = 1210;
    public static final int v1_20 = 1200;
    public static final int v1_19 = 1190;
    public static final int v1_18_1 = 1181;
    public static final int v1_18 = 1180;
    public static final int v1_17 = 1170;
    public static final int v1_16 = 1160;
    public static final int v1_9 = 190;

    public static final boolean HAS_PLAYER_PROFILES = CURRENT_VERSION >= v1_18_1;
    public static final boolean IS_1_8 = CURRENT_VERSION < v1_9;

    private static String version;
    private static String description;
    private static String name;
    private static List<String> authors;

    /** Folia, Paper, Spigot Detection Methods */
    public static boolean isFolia() {
        return zAPI.getFoliaLib().isFolia();
    }

    public static boolean isPaper() {
        return zAPI.getFoliaLib().isPaper();
    }

    public static boolean isSpigot() {
        return zAPI.getFoliaLib().isSpigot();
    }

    public static String getVersion() {
        if(version != null) return version;
        try {
            version = zAPI.getPlugin().getPluginMeta().getVersion();
        }catch (NoSuchMethodError error) {
            version = zAPI.getPlugin().getDescription().getVersion();
        }
        return version;
    }

    public static String getDescription() {
        if(description != null) return description;
        try {
            description = zAPI.getPlugin().getPluginMeta().getDescription();
        }catch (NoSuchMethodError error) {
            description = zAPI.getPlugin().getDescription().getDescription();
        }
        return description;
    }

    public static String getName() {
        if(name != null) return name;
        try {
            name = zAPI.getPlugin().getPluginMeta().getName();
        }catch (NoSuchMethodError error) {
            name = zAPI.getPlugin().getDescription().getName();
        }
        return name;
    }

    public static List<String> getAuthors() {
        if(authors != null) return authors;
        try {
            authors = zAPI.getPlugin().getPluginMeta().getAuthors();
        }catch (NoSuchMethodError error) {
            authors = zAPI.getPlugin().getDescription().getAuthors();
        }
        return authors;
    }

    public static String getMainClass() {
        try{
            return zAPI.getPlugin().getPluginMeta().getMainClass();
        }catch (NoSuchMethodError error) {
            return zAPI.getPlugin().getDescription().getMainClass();
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
