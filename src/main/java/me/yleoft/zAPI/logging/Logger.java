package me.yleoft.zAPI.utility;

import me.yleoft.zAPI.zAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class Logger {

    private static final JavaPlugin plugin = zAPI.getPlugin();
    private boolean debugMode = false;
    private String prefix;

    public Logger(String prefix) {
        setPrefix(prefix);
    }
    public Logger() {
        this("");
    }

    public void info(String message) {
        plugin.getComponentLogger().info(formatTransform(message));
    }

    public void warn(String message) {
        plugin.getComponentLogger().warn(formatTransform(message));
    }

    public void warn(String message, Throwable throwable) {
        plugin.getComponentLogger().warn(formatTransform(message), throwable);
    }

    public void error(String message) {
        plugin.getComponentLogger().error(formatTransform(message));
    }

    public void error(String message, Throwable throwable) {
        plugin.getComponentLogger().error(formatTransform(message), throwable);
    }

    public void debug(String message) {
        if(!debugMode) return;
        plugin.getComponentLogger().debug(formatTransform(message));
    }

    public void debug(String message, Throwable throwable) {
        if(!debugMode) return;
        plugin.getComponentLogger().debug(formatTransform(message), throwable);
    }

    public void trace(String message) {
        plugin.getComponentLogger().trace(formatTransform(message));
    }

    public void trace(String message, Throwable throwable) {
        plugin.getComponentLogger().trace(formatTransform(message), throwable);
    }

    /** Sets the prefix for log messages.
     * @param prefix The prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /** Gets the current prefix for log messages.
     * @return The current prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /** Sets the debug mode for the logger.
     * @param debugMode true to enable debug mode, false to disable
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /** Checks if the logger is in debug mode.
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    private Component formatTransform(String message) {
        return TextFormatter.transform(format(message));
    }

    private String format(String message) {
        return getPrefix().isEmpty() ? message : getPrefix() + " " + message;
    }

}
