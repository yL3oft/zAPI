package me.yleoft.zAPI.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YAMLBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void build_createsFile_andWritesDefaults_andVersionAtBottom() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .header("Header line 1", "Header line 2")
                .footer("Footer line")
                .addDefault("a", "hello")
                .addDefault("b", 5)
                .addDefault("c", true)
                .updateVersion("1.2.3");

        b.build();

        assertTrue(b.getFile().exists());

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);

        assertTrue(content.contains("# Header line 1"));
        assertTrue(content.contains("a: \"hello\""));
        assertTrue(content.contains("b: 5"));
        assertTrue(content.contains("c: true"));

        // version always at bottom
        assertTrue(content.trim().endsWith("config-version: \"1.2.3\""));
    }

    @Test
    void build_preservesExistingUserValue_overDefault() throws Exception {
        Path file = tempDir.resolve("config.yml");

        Files.writeString(file, ""
                + "a: \"user\"\n"
                + "config-version: \"1.0.0\"\n", StandardCharsets.UTF_8);

        YAMLBuilder b = new YAMLBuilder(file.toFile())
                .addDefault("a", "default")
                .addDefault("b", "newDefault")
                .updateVersion("1.0.1")
                .build()
                .refresh();

        assertEquals("user", b.getString("a"));      // preserved
        assertEquals("newDefault", b.getString("b"));// added
        assertEquals("1.0.1", b.version());          // updated
    }

    @Test
    void move_movesExistingValue_andDefaults() {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("old.path", "default")
                .move("old.path", "new.path")
                .build()
                .refresh();

        assertNull(b.getString("old.path"));
        assertEquals("default", b.getString("new.path"));
    }

    @Test
    void voidPath_removesValueFromFile_andDefaults() {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("keep", "yes")
                .addDefault("remove.me", "no")
                .voidPath("remove.me")
                .build()
                .refresh();

        assertEquals("yes", b.getString("keep"));
        assertNull(b.getString("remove.me"));

        String content;
        try {
            content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(content.contains("keep: \"yes\""));
        assertFalse(content.contains("remove.me"));
    }

    @Test
    void multiline_default_isWrittenAsYamlBlock_andReadBackAsJoinedLines() {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("motd", "line1", "line2", "line3")
                .build()
                .refresh();

        assertEquals(List.of("line1", "line2", "line3"), b.getStringList("motd"));

        String content;
        try {
            content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertTrue(content.contains("motd:"));
        assertTrue(content.contains("  - \"line1\""));
        assertTrue(content.contains("  - \"line2\""));
        assertTrue(content.contains("  - \"line3\""));
    }

    @Test
    void list_default_isWritten_andParsedBack() {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("list", List.of("a", "b", "c"))
                .build()
                .refresh();

        assertEquals(List.of("a", "b", "c"), b.getStringList("list"));
    }

    @Test
    void migrateLegacyColors_convertsAmpAndSectionCodes_inStringsAndLists() {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .migrateLegacyColors(true)
                .addDefault("msg", "&aHello §cWorld")
                .addDefault("hex1", "&#ff0000Red")
                .addDefault("hex2", "&x&F&F&0&0&0&0Red")
                .addDefault("list", List.of("&bA", "§eB"))
                .build()
                .refresh();

        assertEquals("<green>Hello <red>World", b.getString("msg"));
        assertEquals("<#ff0000>Red", b.getString("hex1"));
        assertEquals("<#FF0000>Red", b.getString("hex2"));
        assertEquals(List.of("<aqua>A", "<yellow>B"), b.getStringList("list"));
    }

    @Test
    void versionParsing_readsExistingVersionFromFileOnConstruct() throws Exception {
        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, ""
                + "something: \"x\"\n"
                + "config-version: \"2.3.4\"\n", StandardCharsets.UTF_8);

        YAMLBuilder b = new YAMLBuilder(file.toFile());
        assertEquals("2.3.4", b.version());
    }

    // ==================== NEW TESTS: runtime setX() methods ====================

    @Test
    void runtimeSetString_updatesExistingFile_afterBuild_andIsReadable() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("a", "hello")
                .updateVersion("1.0.0")
                .build()
                .refresh();

        b.setString("a", "changed").refresh();

        assertEquals("changed", b.getString("a"));

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("a: \"changed\""));
        assertTrue(content.trim().endsWith("config-version: \"1.0.0\""));
    }

    @Test
    void runtimeSetBooleanAndInt_updatesExistingFile_afterBuild() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("flag", false)
                .addDefault("num", 1)
                .updateVersion("1.0.0")
                .build()
                .refresh();

        b.setBoolean("flag", true);
        b.setInt("num", 42);
        b.refresh();

        assertTrue(b.getBoolean("flag"));
        assertEquals(42, b.getInt("num"));

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("flag: true"));
        assertTrue(content.contains("num: 42"));
    }

    @Test
    void runtimeUnset_removesKeyFromFile_andGetReturnsNull_orDefaultIfPresent() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("keep", "yes")
                .addDefault("remove", "no")
                .updateVersion("1.0.0")
                .build()
                .refresh();

        // Ensure it's there first
        assertEquals("no", b.getString("remove"));

        b.unset("remove").refresh();

        // After unset, should not exist in cached data -> falls back to default (because default still exists)
        // This documents current expected behavior: getValue checks cachedData first, then defaults.
        assertEquals("no", b.getString("remove"));

        // But the file should NOT contain it anymore (because we removed it from cachedData and rewrote file)
        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertFalse(content.contains("remove:"));
        assertTrue(content.contains("keep: \"yes\""));
    }

    @Test
    void runtimeSetMultiline_writesYamlBlock_andReadBackAsJoinedLines() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("motd", "original")
                .build()
                .refresh();

        b.setMultiline("motd", "line1", "line2", "line3").refresh();

        assertEquals("line1\nline2\nline3", b.getString("motd"));

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("motd: |"));
        assertTrue(content.contains("  line1"));
        assertTrue(content.contains("  line2"));
        assertTrue(content.contains("  line3"));
    }

    @Test
    void runtimeSetStringList_updatesList_andParsedBack() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("list", List.of("a"))
                .build()
                .refresh();

        b.setStringList("list", List.of("x", "y")).refresh();

        assertEquals(List.of("x", "y"), b.getStringList("list"));

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("list:"));
        assertTrue(content.contains("- \"x\""));
        assertTrue(content.contains("- \"y\""));
    }

    @Test
    void runtimeSet_respectsMigrateLegacyColors_whenEnabled() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .migrateLegacyColors(true)
                .addDefault("msg", "x")
                .build()
                .refresh();

        b.setString("msg", "&aHello §cWorld").refresh();

        assertEquals("<green>Hello <red>World", b.getString("msg"));

        String content = Files.readString(b.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("msg: \"<green>Hello <red>World\""));
    }

    @Test
    void runtimeSet_worksWithMoveAppliedBeforeSet() throws Exception {
        YAMLBuilder b = new YAMLBuilder(tempDir.toFile(), "config.yml")
                .addDefault("old.path", "default")
                .move("old.path", "new.path")
                .build()
                .refresh();

        // after move, setting old.path should write to old.path (since it's a new set)
        // but applyPendingMoves is called in set(), so cached structure should already be moved
        // We document expected behavior: user sets old.path explicitly -> it should exist.
        b.setString("old.path", "userValue").refresh();

        assertEquals("default", b.getString("new.path"));
        assertEquals("userValue", b.getString("old.path"));
    }
}