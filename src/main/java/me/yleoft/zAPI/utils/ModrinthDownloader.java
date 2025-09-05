package me.yleoft.zAPI.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ModrinthDownloader is a utility class for downloading the latest version of a Modrinth project.
 * It retrieves the download URL of the latest version's first file.
 */
public class ModrinthDownloader {

    private static final String BASE = "https://api.modrinth.com/v2";
    private static final Gson GSON = new Gson();

    /**
     * @param projectIdentifier Modrinth project slug or project ID
     * @return direct download URL of the newest version's first file
     */
    public static String getLatestDownloadUrl(String projectIdentifier) throws IOException {
        Project project = getJson(
                BASE + "/project/" + encode(projectIdentifier),
                Project.class
        );
        if (project == null || project.versions == null || project.versions.isEmpty()) {
            throw new IOException("No versions found for project: " + projectIdentifier);
        }

        String latestVersionId = project.versions.get(project.versions.size()-1);
        Version version = getJson(
                BASE + "/version/" + encode(latestVersionId),
                Version.class
        );
        if (version == null || version.files == null || version.files.isEmpty()) {
            throw new IOException("No files for version: " + latestVersionId);
        }

        return version.files.get(0).url;
    }

    /**
     * Fetches JSON from the specified URL and deserializes it into the specified class.
     * @param url URL to fetch JSON from
     * @param clazz class to deserialize JSON into
     * @return deserialized JSON object
     */
    private static <T> T getJson(String url, Class<T> clazz) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "YourAppName/1.0 (contact@example.com)");
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " from " + url);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return GSON.fromJson(br, clazz);
        } finally {
            conn.disconnect();
        }
    }

    private static String encode(String s) {
        return s;
    }

    static class Project {
        List<String> versions;
    }

    static class Version {
        List<FileInfo> files;
    }

    static class FileInfo {
        String url;
        String filename;
        boolean primary;
        @SerializedName("hashes")
        Hashes hashes;
    }

    static class Hashes {
        String sha1;
        String sha512;
    }
}
