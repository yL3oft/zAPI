package me.yleoft.zAPI.utils;

public abstract class ConfigUtils {

    /**
     * Forms a yaml path from the given strings.
     * @param strs The strings to form the path from.
     * @return The formed path.
     */
    public static String formPath(String... strs) {
        StringBuilder path = new StringBuilder();

        try {
            for(String str : strs) {
                if(str.isEmpty()) continue;
                if(path.isEmpty()) {
                    path = new StringBuilder(str);
                    continue;
                }
                path.append(".").append(str);
            }
        }catch (Exception ignored) {
        }

        return path.toString();
    }

}
