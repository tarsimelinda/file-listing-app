package com.codecool.backend.service;

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

    public List<String> listFiles(String requestedPath, String extension) {
        Path startPath = Path.of(requestedPath).normalize();

        if (!Files.exists(startPath)) {
            throw new IllegalArgumentException("Path does not exist: " + requestedPath);
        }

        if (!Files.isDirectory(startPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + requestedPath);
        }

        String normalizedExtension = normalizeExtension(extension);

        Set<Path> files = new HashSet<>();
        collectFiles(startPath, files, normalizedExtension);

        return files.stream()
                .map(startPath::relativize)
                .map(Path::toString)
                .map(path -> path.replace("\\", "/"))
                .toList();
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

        return extension.trim().replaceFirst("^\\.", "");
    }
}