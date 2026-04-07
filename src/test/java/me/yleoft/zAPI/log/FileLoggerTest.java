package me.yleoft.zAPI.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class FileLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void compressLogs_returnsNull_whenDirectoryHasNoLogFiles() throws Exception {
        Files.writeString(tempDir.resolve("notes.txt"), "not a log", StandardCharsets.UTF_8);

        File compressed = FileLogger.compressLogs(tempDir.toFile(), FileLogger.getLogFiles(tempDir.toFile()));

        assertNull(compressed);
        assertEquals(0, zipFilesIn(tempDir).size());
    }

    @Test
    void compressLogs_createsZipOnlyFromExistingLogs_andDeletesOriginalLogs() throws Exception {
        Path firstLog = Files.writeString(tempDir.resolve("latest.log"), "first line\nsecond line", StandardCharsets.UTF_8);
        Path secondLog = Files.writeString(tempDir.resolve("debug.log"), "debug entry", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("notes.txt"), "ignore me", StandardCharsets.UTF_8);

        File compressed = FileLogger.compressLogs(tempDir.toFile(), FileLogger.getLogFiles(tempDir.toFile()));

        assertNotNull(compressed);
        assertTrue(compressed.exists());
        assertFalse(Files.exists(firstLog));
        assertFalse(Files.exists(secondLog));
        assertTrue(Files.exists(tempDir.resolve("notes.txt")));

        try (ZipFile zipFile = new ZipFile(compressed)) {
            assertNotNull(zipFile.getEntry("latest.log"));
            assertNotNull(zipFile.getEntry("debug.log"));
            assertEquals("first line\nsecond line", new String(zipFile.getInputStream(zipFile.getEntry("latest.log")).readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("debug entry", new String(zipFile.getInputStream(zipFile.getEntry("debug.log")).readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void compressLogs_deletesOldestZipArchives_whenArchiveLimitWouldBeExceeded() throws Exception {
        long baseTime = System.currentTimeMillis() - 100_000L;
        File oldestArchive = null;
        for (int i = 0; i < FileLogger.MAX_ZIPPED_LOG_FILES; i++) {
            Path zipPath = tempDir.resolve(String.format("archive-%02d.zip", i));
            Files.write(zipPath, new byte[]{(byte) i});
            File zipFile = zipPath.toFile();
            assertTrue(zipFile.setLastModified(baseTime + i), "Expected to control archive ordering for the test");
            if (i == 0) {
                oldestArchive = zipFile;
            }
        }

        Path logFile = Files.writeString(tempDir.resolve("session.log"), "new session", StandardCharsets.UTF_8);

        File compressed = FileLogger.compressLogs(tempDir.toFile(), FileLogger.getLogFiles(tempDir.toFile()));

        assertNotNull(compressed);
        assertTrue(compressed.exists());
        assertNotNull(oldestArchive);
        assertFalse(oldestArchive.exists(), "The oldest archive should be deleted to make room for the new one");
        assertFalse(Files.exists(logFile));
        assertEquals(FileLogger.MAX_ZIPPED_LOG_FILES, zipFilesIn(tempDir).size());
    }

    private List<Path> zipFilesIn(Path folder) throws Exception {
        try (var stream = Files.list(folder)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }
}

