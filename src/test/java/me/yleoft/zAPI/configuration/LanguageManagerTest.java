package me.yleoft.zAPI.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageManagerTest {

    @TempDir
    Path tempDir;

    private YAMLBuilder makeLanguage(String code) {
        return new YAMLBuilder(tempDir.toFile(), code + ".yml");
    }

    @Test
    void get_returnsMainLanguageValue_whenPresent() throws Exception {
        YAMLBuilder en = makeLanguage("en")
                .addDefault("prefix", "[EN]")
                .addDefault("messages.hello", "Hello")
                .build()
                .refresh();

        YAMLBuilder de = makeLanguage("de")
                .addDefault("prefix", "[DE]")
                .addDefault("messages.hello", "Hallo")
                .build()
                .refresh();

        LanguageManager manager = new LanguageManager(List.of(en, de), "en", "de");

        assertEquals("[EN]", manager.getString("prefix"));
        assertEquals("Hello", manager.getString("messages.hello"));
    }

    @Test
    void get_fallsBackToFallbackLanguage_whenMissingInMain() throws Exception {
        YAMLBuilder en = makeLanguage("en")
                .addDefault("messages.only_en", "Only in EN")
                .build()
                .refresh();

        YAMLBuilder de = makeLanguage("de")
                .addDefault("messages.shared", "Gemeinsam")
                .addDefault("messages.only_de", "Nur DE")
                .build()
                .refresh();

        LanguageManager manager = new LanguageManager(List.of(en, de), "en", "de");

        // missing in EN -> fallback to DE
        assertEquals("Nur DE", manager.getString("messages.only_de"));

        // still present in EN -> use EN
        assertEquals("Only in EN", manager.getString("messages.only_en"));

        // missing in EN but exists in DE
        assertEquals("Gemeinsam", manager.getString("messages.shared"));
    }

    @Test
    void get_returnsNull_whenMissingInBoth() throws Exception {
        YAMLBuilder en = makeLanguage("en").addDefault("a", "1").build().refresh();
        YAMLBuilder de = makeLanguage("de").addDefault("b", "2").build().refresh();

        LanguageManager manager = new LanguageManager(List.of(en, de), "en", "de");

        assertNull(manager.get("does.not.exist"));
        assertNull(manager.getString("does.not.exist"));
    }

    @Test
    void reload_reloadsFromDisk_afterFileChanges() throws Exception {
        YAMLBuilder en = makeLanguage("en")
                .addDefault("messages.hello", "Hello")
                .build()
                .refresh();

        LanguageManager manager = new LanguageManager(List.of(en), "en");

        assertEquals("Hello", manager.getString("messages.hello"));

        en.setString("messages.hello", "Hello (updated)").refresh();

        manager.reload();

        assertEquals("Hello (updated)", manager.getString("messages.hello"));
    }

    @Test
    void loadsNestedYaml_andFlattensKeysCorrectly() throws Exception {
        YAMLBuilder en = makeLanguage("en")
                .addDefault("ui.title", "Title")
                .addDefault("ui.buttons.ok", "OK")
                .addDefault("ui.buttons.cancel", "Cancel")
                .build()
                .refresh();

        LanguageManager manager = new LanguageManager(List.of(en), "en");

        assertEquals("Title", manager.getString("ui.title"));
        assertEquals("OK", manager.getString("ui.buttons.ok"));
        assertEquals("Cancel", manager.getString("ui.buttons.cancel"));
    }

    @Test
    void getCurrentLanguage_andGetFallbackLanguage_returnConstructorValues() throws Exception {
        YAMLBuilder en = makeLanguage("en").addDefault("x", "y").build().refresh();
        YAMLBuilder de = makeLanguage("de").addDefault("x", "z").build().refresh();

        LanguageManager manager = new LanguageManager(List.of(en, de), "en", "de");

        assertEquals("en", manager.getCurrentLanguage());
        assertEquals("de", manager.getFallbackLanguage());
        assertEquals(2, manager.getLanguages().size());
    }
}