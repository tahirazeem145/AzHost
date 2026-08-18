package com.azhost.deployment.security;

import java.util.Map;
import java.util.Set;

public class DeploymentSecurityPolicy {

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".jsp", ".php", ".py", ".rb", ".cgi", ".exe", ".sh", ".bat", ".cmd", ".pl", ".dll", ".so"
    );

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".htm", "text/html; charset=utf-8"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".js", "text/javascript; charset=utf-8"),
            Map.entry(".mjs", "text/javascript; charset=utf-8"),
            Map.entry(".json", "application/json"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".txt", "text/plain; charset=utf-8"),
            Map.entry(".wasm", "application/wasm"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".woff2", "font/woff2"),
            Map.entry(".ttf", "font/ttf"),
            Map.entry(".map", "application/json")
    );

    public static boolean isForbiddenFileExtension(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return FORBIDDEN_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    public static String getMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex != -1) {
            String ext = lower.substring(dotIndex);
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }

    private DeploymentSecurityPolicy() {}
}
