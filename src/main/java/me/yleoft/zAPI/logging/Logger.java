package me.yleoft.zAPI.logging;

import me.yleoft.zAPI.utility.TextFormatter;
import me.yleoft.zAPI.zAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Logger {

    private static final JavaPlugin plugin = zAPI.getPlugin();
    private boolean debugMode = false;
    private Component prefix;

    public Logger(String prefix) {
        setPrefix(prefix);
    }

    public Logger(Component prefix) {
        setPrefix(prefix);
    }

    public Logger() {}

    public void info(String message) {
        logMultiline(message, (line) -> plugin.getComponentLogger().info(line));
    }

    public void warn(String message) {
        logMultiline(message, (line) -> plugin.getComponentLogger().warn(line));
    }

    public void warn(String message, Throwable throwable) {
        logMultilineThrowable(message, throwable,
                (line) -> plugin.getComponentLogger().warn(line),
                (line, t) -> plugin.getComponentLogger().warn(line, t));
    }

    public void error(String message) {
        logMultiline(message, (line) -> plugin.getComponentLogger().error(line));
    }

    public void error(String message, Throwable throwable) {
        logMultilineThrowable(message, throwable,
                (line) -> plugin.getComponentLogger().error(line),
                (line, t) -> plugin.getComponentLogger().error(line, t));
    }

    public void debug(String message) {
        if (!debugMode) return;
        logMultiline(message, (line) -> plugin.getComponentLogger().debug(line));
    }

    public void debug(String message, Throwable throwable) {
        if (!debugMode) return;
        logMultilineThrowable(message, throwable,
                (line) -> plugin.getComponentLogger().debug(line),
                (line, t) -> plugin.getComponentLogger().debug(line, t));
    }

    public void trace(String message) {
        logMultiline(message, (line) -> plugin.getComponentLogger().trace(line));
    }

    public void trace(String message, Throwable throwable) {
        logMultilineThrowable(message, throwable,
                (line) -> plugin.getComponentLogger().trace(line),
                (line, t) -> plugin.getComponentLogger().trace(line, t));
    }

    /** Sets the prefix for log messages. */
    public void setPrefix(Component prefix) {
        this.prefix = prefix;
    }

    /** Sets the prefix for log messages. */
    public void setPrefix(String prefix) {
        this.prefix = TextFormatter.transform(prefix);
    }

    /** Gets the current prefix for log messages. */
    public Component getPrefix() {
        return prefix;
    }

    /** Sets the debug mode for the logger. */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /** Checks if the logger is in debug mode. */
    public boolean isDebugMode() {
        return debugMode;
    }

    // -------------------------
    // Multiline handling
    // -------------------------

    private void logMultiline(String message, Consumer<Component> sink) {
        if (message == null) return;

        String[] lines = splitLinesTrimOuterBlanks(message);
        for (String line : lines) {
            sink.accept(formatTransform(line));
        }
    }

    private void logMultilineThrowable(
            String message,
            Throwable throwable,
            Consumer<Component> sinkNoThrowable,
            BiConsumer<Component, Throwable> sinkWithThrowable
    ) {
        if (message == null) return;

        String[] lines = splitLinesTrimOuterBlanks(message);
        boolean includeThrowable = debugMode && throwable != null;

        for (int i = 0; i < lines.length; i++) {
            Component formatted = formatTransform(lines[i]);
            if (includeThrowable && i == lines.length - 1) {
                sinkWithThrowable.accept(formatted, throwable);
            } else {
                sinkNoThrowable.accept(formatted);
            }
        }
    }

    /**
     * Splits a message into lines while removing accidental leading/trailing blank lines
     * (common with Java text blocks and copy/paste), but preserving internal empty lines.
     */
    private static String[] splitLinesTrimOuterBlanks(String message) {
        String normalized = message.stripTrailing();

        String[] lines = normalized.split("\\R", -1);
        int start = 0;
        while (start < lines.length && lines[start].isEmpty()) start++;
        int end = lines.length;
        while (end > start && lines[end - 1].isEmpty()) end--;

        return Arrays.copyOfRange(lines, start, end);
    }

    private Component formatTransform(String message) {
        return TextFormatter.transform(format(message));
    }

    private String format(String message) {
        return getPrefix() == null ? message : zAPI.getMiniMessage().serialize(getPrefix()) + " " + message;
    }
}