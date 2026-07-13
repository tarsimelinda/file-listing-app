package com.codecool.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileListingService {

    private final Path inputRoot;

    public FileListingService(@Value("${app.input-root}") String inputRoot) {
        this.inputRoot = Path.of(inputRoot).toAbsolutePath().normalize();
    }

    public List<String> listFiles(String requestedPath, String extension) {
        validateRequest(requestedPath);

        Path startPath = resolveAndValidatePath(requestedPath);

        String normalizedExtension = normalizeExtension(extension);

        Set<Path> files = new HashSet<>();
        collectFiles(startPath, files, normalizedExtension);

        return files.stream()
                .map(startPath::relativize)
                .map(Path::toString)
                .map(path -> path.replace("\\", "/"))
                .sorted()
                .toList();
    }

    private Path resolveAndValidatePath(String requestedPath) {
        Path resolvedPath = Path.of(requestedPath).toAbsolutePath().normalize();

        if (!resolvedPath.startsWith(inputRoot)) {
            throw new IllegalArgumentException("Path must be under input root: " + inputRoot);
        }

        if (!Files.exists(resolvedPath)) {
            throw new IllegalArgumentException("Path does not exist: " + requestedPath);
        }

        if (!Files.isDirectory(resolvedPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + requestedPath);
        }

        return resolvedPath;
    }

    private void validateRequest(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
    }

    private void collectFiles(Path currentPath, Set<Path> files, String extension) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(currentPath)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    collectFiles(entry, files, extension);
                } else if (Files.isRegularFile(entry) && matchesExtension(entry, extension)) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read directory: " + currentPath, e);
        }
    }

    private boolean matchesExtension(Path path, String extension) {
        if (extension == null || extension.isBlank()) {
            return true;
        }

        String fileName = path.getFileName().toString();

        return fileName.endsWith("." + extension);
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }

        String normalizedExtension = extension.trim().replaceFirst("^\\.", "");

        if (normalizedExtension.contains("/") || normalizedExtension.contains("\\")) {
            throw new IllegalArgumentException("Extension must not contain path separators.");
        }

        return normalizedExtension;
    }
}