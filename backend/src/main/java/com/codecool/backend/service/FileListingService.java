package com.codecool.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileListingService {

    private final Path inputRoot;

    public FileListingService(@Value("${app.input-root}") String inputRoot) {
        this.inputRoot = Path.of(inputRoot).toAbsolutePath().normalize();
    }

    /**
     * Recursively lists unique file names under the requested directory
     * and filters them by extension.
     *
     * @param requestedPath the directory path to search under
     * @param extension optional file extension filter without or with leading dot
     * @return sorted unique file names matching the requested extension
     */
    public List<String> listFiles(String requestedPath, String extension) {
        Path startPath = resolvePath(requestedPath);
        String normalizedExtension = normalizeExtension(extension);

        try (Stream<Path> paths = Files.walk(startPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesExtension(path, normalizedExtension))
                    .map(path -> path.getFileName().toString())
                    .distinct()
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("Failed to list files under: " + requestedPath, e);
        }
    }

    private Path resolvePath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("Path must not be empty.");
        }

        try {
            Path realInputRoot = inputRoot.toRealPath().normalize();
            Path resolvedPath = Path.of(requestedPath).toRealPath().normalize();

            if (!resolvedPath.startsWith(realInputRoot)) {
                throw new IllegalArgumentException("Path must be under input root: " + realInputRoot);
            }

            if (!Files.isDirectory(resolvedPath)) {
                throw new IllegalArgumentException("Path must be an existing directory: " + requestedPath);
            }

            return resolvedPath;
        } catch (IOException e) {
            throw new IllegalArgumentException("Path does not exist or cannot be accessed: " + requestedPath);
        }
    }

    private boolean matchesExtension(Path path, String extension) {
        return extension.isBlank()
                || path.getFileName().toString().endsWith("." + extension);
    }

    private String normalizeExtension(String extension) {
        String normalizedExtension = extension == null
                ? ""
                : extension.trim().replaceFirst("^\\.", "");

        if (normalizedExtension.contains("/") || normalizedExtension.contains("\\")) {
            throw new IllegalArgumentException("Extension must not contain path separators.");
        }

        return normalizedExtension;
    }
}