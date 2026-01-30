package me.yleoft.zAPI.utility;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * External dependency manager for Java 17:
 * - Register JAR dependencies by direct URL
 * - Download into a user-provided static directory
 * - Load into isolated classloaders to avoid version conflicts
 *
 * This intentionally does NOT relocate/shade packages. Isolation avoids conflicts without bytecode rewriting.
 */
public final class ExternalDependencyManager {

    /** Application must set this. */
    public static volatile File STATIC_DIRECTORY = null;

    private final HttpClient httpClient;
    private final Map<String, RegisteredDependency> registry = new ConcurrentHashMap<>();

    public ExternalDependencyManager() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    public ExternalDependencyManager(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    /** Register a dependency by ID and JAR URL. */
    public void registerDependency(String id, URI jarUrl) {
        validateId(id);
        Objects.requireNonNull(jarUrl, "jarUrl");
        if (!jarUrl.toString().endsWith(".jar")) {
            throw new IllegalArgumentException("jarUrl must point to a .jar: " + jarUrl);
        }
        registry.put(id, new RegisteredDependency(id, jarUrl));
    }

    public boolean isRegistered(String id) {
        return registry.containsKey(id);
    }

    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public RegisteredDependency getRegistered(String id) {
        RegisteredDependency dep = registry.get(id);
        if (dep == null) throw new IllegalArgumentException("Unknown dependency id: " + id);
        return dep;
    }

    /**
     * Download a dependency into:
     *   STATIC_DIRECTORY/downloads/<id>-<hash>.jar
     *
     * Simple caching: if file exists and is non-empty, it is reused.
     */
    public Path download(String id) throws IOException, InterruptedException {
        RegisteredDependency dep = getRegistered(id);
        Path baseDir = ensureStaticDir();

        String fileName = safeFileName(dep.id()) + "-" + shortHash(dep.jarUrl().toString()) + ".jar";
        Path out = baseDir.resolve("downloads").resolve(fileName);
        Files.createDirectories(out.getParent());

        if (Files.exists(out) && Files.size(out) > 0) {
            return out;
        }

        HttpRequest req = HttpRequest.newBuilder(dep.jarUrl())
                .GET()
                .timeout(Duration.ofMinutes(2))
                .build();

        Path tmp = out.resolveSibling(out.getFileName() + ".part");
        Files.deleteIfExists(tmp);

        HttpResponse<Path> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            Files.deleteIfExists(tmp);
            throw new IOException("Failed to download " + dep.jarUrl() + " (HTTP " + resp.statusCode() + ")");
        }

        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return out;
    }

    public Map<String, Path> downloadAll() throws IOException, InterruptedException {
        Map<String, Path> result = new LinkedHashMap<>();
        for (String id : sortedIds()) {
            result.put(id, download(id));
        }
        return result;
    }

    /**
     * Loads ONE dependency into its own isolated classloader.
     * This is the strongest way to avoid version conflicts between external dependencies.
     *
     * Note: types loaded in this classloader should not “leak” across boundaries.
     * Communicate via your own API interfaces loaded by the parent.
     */
    public URLClassLoader loadIsolated(String id, ClassLoader parent) throws IOException, InterruptedException {
        Path jar = download(id);
        URL url = jar.toUri().toURL();
        ClassLoader p = (parent != null) ? parent : Thread.currentThread().getContextClassLoader();
        return new URLClassLoader(new URL[]{url}, p);
    }

    /**
     * Loads ALL dependencies into ONE isolated classloader (still isolated from the app).
     * Use this when the external jars need to see each other.
     */
    public URLClassLoader loadAllIntoOneIsolatedLoader(ClassLoader parent) throws IOException, InterruptedException {
        List<URL> urls = new ArrayList<>();
        for (String id : sortedIds()) {
            urls.add(download(id).toUri().toURL());
        }
        ClassLoader p = (parent != null) ? parent : Thread.currentThread().getContextClassLoader();
        return new URLClassLoader(urls.toArray(URL[]::new), p);
    }

    /**
     * Convenience: load all dependencies as separate isolated classloaders.
     */
    public Map<String, URLClassLoader> loadEachIsolated(ClassLoader parent) throws IOException, InterruptedException {
        Map<String, URLClassLoader> loaders = new LinkedHashMap<>();
        for (String id : sortedIds()) {
            loaders.put(id, loadIsolated(id, parent));
        }
        return loaders;
    }

    // ---- types ----

    public record RegisteredDependency(String id, URI jarUrl) { }

    // ---- helpers ----

    private Path ensureStaticDir() throws IOException {
        File dir = STATIC_DIRECTORY;
        if (dir == null) {
            throw new IllegalStateException("ExternalDependencyManager.STATIC_DIRECTORY must be set by the user before use.");
        }
        Path p = dir.toPath().toAbsolutePath().normalize();
        Files.createDirectories(p);
        return p;
    }

    private static void validateId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("id is blank");
    }

    private static String safeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static List<String> sortedIdsFrom(Set<String> ids) {
        List<String> list = new ArrayList<>(ids);
        Collections.sort(list);
        return list;
    }

    private List<String> sortedIds() {
        return sortedIdsFrom(registry.keySet());
    }

    private static String shortHash(String in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(in.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}