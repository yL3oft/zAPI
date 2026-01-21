package me.yleoft.zAPI.org.bukkit.configuration.file;

import java.io.File;

/**
 * Test-only stub. YAMLBuilder calls YamlConfiguration.loadConfiguration(file).
 * We don't need real YAML parsing for YAMLBuilder unit tests because YAMLBuilder
 * implements its own parsing/writing for build() and get* methods use cachedData.
 */
public class YamlConfiguration extends FileConfiguration {

    public static YamlConfiguration loadConfiguration(File file) {
        return new YamlConfiguration();
    }
}