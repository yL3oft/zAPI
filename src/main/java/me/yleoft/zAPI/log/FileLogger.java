package me.yleoft.zAPI.log;

import me.yleoft.zAPI.zAPI;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileLogger {

    static final int MAX_ZIPPED_LOG_FILES = 10;

    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /**
     * Creates a LogUtils instance.
     *
     * @param folder The folder where the log file will be stored.
     * @param name   The name of the log file (without extension).
     */
    public FileLogger(File folder, String name) {
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                zAPI.getLogger().warn("Failed to create folder: " + folder.getAbsolutePath());
            }
        }
        this.logFile = new File(folder, name + ".log");
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    zAPI.getLogger().warn("Failed to create log file: " + logFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create log file: " + logFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Writes a message to the log file in the format:
     * [dd/MM/yyyy HH:mm] message
     *
     * @param message The message to log.
     */
    public void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String formattedMessage = "[" + timestamp + "] " + message;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(formattedMessage);
            writer.newLine();
        } catch (IOException e) {
            zAPI.getLogger().warn("Failed to write to log file: " + logFile.getAbsolutePath(), e);
        }
    }

    public File getLogFile() {
        return logFile;
    }

    /**
     * Compresses all existing {@code .log} files in the log folder into a single {@code .zip}
     * archive and deletes the original log files that were archived.
     *
     * @return The compressed {@code .zip} file, or {@code null} when there is no log file to archive.
     */
    public static File compressLogs() {
        File folder = LogManager.getLogFolder();
        return compressLogs(folder, getLogFiles(folder));
    }

    static File compressLogs(File folder, List<File> logFiles) {
        if (folder == null) {
            return null;
        }

        List<File> filesToCompress = logFiles == null ? List.of() : logFiles.stream()
                .filter(file -> file != null && file.exists() && file.isFile() && file.getName().endsWith(".log"))
                .distinct()
                .toList();
        if (filesToCompress.isEmpty()) {
            return null;
        }

        if (!folder.exists() && !folder.mkdirs()) {
            zAPI.getLogger().warn("Failed to create folder: " + folder.getAbsolutePath());
            return null;
        }

        pruneOldZipFiles(folder, MAX_ZIPPED_LOG_FILES - 1);

        File compressedFile = createCompressedFile(folder);

        try (FileOutputStream fos = new FileOutputStream(compressedFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            for (File file : filesToCompress) {
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = bis.read(buffer)) != -1) zos.write(buffer, 0, len);
                    zos.closeEntry();
                }
                if (!file.delete()) {
                    zAPI.getLogger().warn("Could not delete log file: " + file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            if (compressedFile.exists() && !compressedFile.delete()) {
                zAPI.getLogger().warn("Could not delete incomplete log archive: " + compressedFile.getAbsolutePath());
            }
            zAPI.getLogger().warn("Failed to compress log files.", e);
            return null;
        }

        return compressedFile;
    }

    static List<File> getLogFiles(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return List.of();
        }

        File[] logFiles = folder.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null || logFiles.length == 0) {
            return List.of();
        }

        return Arrays.stream(logFiles)
                .filter(File::isFile)
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    static void pruneOldZipFiles(File folder, int maxZipFiles) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }

        int allowedZipFiles = Math.max(0, maxZipFiles);
        File[] zipFiles = folder.listFiles((dir, name) -> name.endsWith(".zip"));
        if (zipFiles == null || zipFiles.length <= allowedZipFiles) {
            return;
        }

        Arrays.sort(zipFiles, Comparator
                .comparingLong(File::lastModified)
                .thenComparing(File::getName));

        int filesToDelete = zipFiles.length - allowedZipFiles;
        for (int i = 0; i < filesToDelete; i++) {
            File zipFile = zipFiles[i];
            if (!zipFile.delete()) {
                zAPI.getLogger().warn("Could not delete old log archive: " + zipFile.getAbsolutePath());
            }
        }
    }

    private static File createCompressedFile(File folder) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File compressedFile = new File(folder, timestamp + ".zip");
        int counter = 1;
        while (compressedFile.exists()) {
            compressedFile = new File(folder, timestamp + "_" + counter++ + ".zip");
        }
        return compressedFile;
    }
}