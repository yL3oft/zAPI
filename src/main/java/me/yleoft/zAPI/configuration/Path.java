package me.yleoft.zAPI.configuration;

import java.util.StringJoiner;

/**
 * Utility class for configuration path-related operations.
 */
public abstract class Path {

    /**
     * Forms a yaml path from the given strings.
     * @param strs The strings to form the path from.
     * @return The formed path.
     */
    public static String formPath(String...  strs) {
        StringJoiner joiner = new StringJoiner(".");
        for (String str : strs) {
            if (str != null && !str.isEmpty()) {
                joiner.add(str);
            }
        }
        return joiner.toString();
    }

}
