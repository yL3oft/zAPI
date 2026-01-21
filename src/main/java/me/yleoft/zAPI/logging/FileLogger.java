package me.yleoft.zAPI.logging;

import me.yleoft.zAPI.zAPI;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileLogger {

    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /**
     * Creates a LogUtils instance.
     *
     * @param folder The folder where the log file will be stored.
     * @param name   The name of the log file (without extension).
     * @throws IOException If the folder cannot be created or the file cannot be accessed.
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
     * Compresses all log files from a list of LogUtils into a single .log.gz file
     * and deletes the original logs.
     *
     * @return The compressed .log.gz file
     */
    public static File compressLogs() {
        File folder = LogManager.getLogFolder();
        List<FileLogger> logs = LogManager.getFiles();
        if (logs.isEmpty()) {
            return null;
        }
        if (!folder.exists() && !folder.mkdirs()) {
            zAPI.getLogger().warn("Failed to create folder: " + folder.getAbsolutePath());
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
        File compressedFile = new File(folder, timestamp + ".zip");
        try (FileOutputStream fos = new FileOutputStream(compressedFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            for (FileLogger log : logs) {
                File file = log.getLogFile();
                if (!file.exists()) continue;
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {

                    ZipEntry entry = new ZipEntry(file.getName());
                    zos.putNextEntry(entry);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
                if (!file.delete()) {
                    zAPI.getLogger().warn("Could not delete log file: " + file.getAbsolutePath());
                }
            }

        } catch (IOException e) {
            zAPI.getLogger().warn("Failed to compress log files.", e);
            return null;
        }

        return compressedFile;
    }
}