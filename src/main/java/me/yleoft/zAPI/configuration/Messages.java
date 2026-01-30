package me.yleoft.zAPI.configuration;

/**
 * Class for managing customizable messages in the application.
 */
public abstract class Messages {

    private static String pluginPrefix = "";
    private static String noPermissionDefault = "<red>You do not have permission to execute this command.";
    private static String onlyPlayersDefault = "<red>This command can only be executed by players.";
    private static String cooldownMessage = "<red>You must wait %time% seconds before using this command again.";

    /** GETTERS & SETTERS **/
    public static String getCooldownMessage(double time) {
        return cooldownMessage.replace("%time%", String.valueOf(time));
    }

    public static void setCooldownMessage(String cooldownMessage) {
        Messages.cooldownMessage = cooldownMessage;
    }

    public static String getPluginPrefix() {
        return pluginPrefix;
    }

    public static void setPluginPrefix(String pluginPrefix) {
        Messages.pluginPrefix = pluginPrefix;
    }

    public static String getNoPermissionDefault() {
        return noPermissionDefault;
    }

    public static void setNoPermissionDefault(String noPermissionDefault) {
        Messages.noPermissionDefault = noPermissionDefault;
    }

    public static String getOnlyPlayersDefault() {
        return onlyPlayersDefault;
    }

    public static void setOnlyPlayersDefault(String onlyPlayersDefault) {
        Messages.onlyPlayersDefault = onlyPlayersDefault;
    }
}
