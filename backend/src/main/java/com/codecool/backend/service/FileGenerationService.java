package com.codecool.backend.service;

import com.codecool.backend.dto.GenerateRequest;
import com.codecool.backend.dto.GenerateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class FileGenerationService {

    private static final int MAX_DEPTH = 100;
    private static final int MAX_FILES_PER_DIRECTORY = 20;

    private final Path inputRoot;

    public FileGenerationService(@Value("${app.input-root}") String inputRoot) {
        this.inputRoot = Path.of(inputRoot).toAbsolutePath().normalize();
    }

    /**
     * Generates a deep directory structure under the requested base path.
     * The previous generated structure is removed before creating the new one.
     *
     * @param request generation parameters including base path, depth, files per directory and extension
     * @return summary of the generated directory structure
     */
    public GenerateResponse generate(GenerateRequest request) {
        validateRequest(request);

        Path basePath = resolveBasePath(request.basePath());
        Path generatedRoot = basePath.resolve("generated");
        Path currentPath = generatedRoot;
        String extension = normalizeExtension(request.extension());

        try {
            deleteDirectoryIfExists(generatedRoot);
            Files.createDirectories(generatedRoot);

            for (int level = 1; level <= request.depth(); level++) {
                currentPath = currentPath.resolve(toAlphabeticName(level));
                Files.createDirectories(currentPath);

                for (int fileNumber = 1; fileNumber <= request.filesPerDirectory(); fileNumber++) {
                    String fileName = fileNumber + "." + extension;
                    Files.writeString(currentPath.resolve(fileName), "Generated file: " + fileName);
                }
            }

            return new GenerateResponse(
                    request.basePath(),
                    request.depth(),
                    request.depth(),
                    request.depth() * request.filesPerDirectory(),
                    generatedRoot.relativize(currentPath).toString().replace("\\", "/")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate file structure under: " + request.basePath(), e);
        }
    }

    private void deleteDirectoryIfExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private void validateRequest(GenerateRequest request) {
        if (request.basePath() == null || request.basePath().isBlank()) {
            throw new IllegalArgumentException("Base path must not be empty.");
        }

        if (request.depth() <= 0 || request.depth() > MAX_DEPTH) {
            throw new IllegalArgumentException("Depth must be between 1 and " + MAX_DEPTH + ".");
        }

        if (request.filesPerDirectory() < 0 || request.filesPerDirectory() > MAX_FILES_PER_DIRECTORY) {
            throw new IllegalArgumentException("Files per directory must be between 0 and " + MAX_FILES_PER_DIRECTORY + ".");
        }
    }

    private Path resolveBasePath(String requestedBasePath) {
        try {
            Path realInputRoot = inputRoot.toRealPath().normalize();
            Path basePath = Path.of(requestedBasePath).toRealPath().normalize();

            if (!basePath.startsWith(realInputRoot)) {
                throw new IllegalArgumentException("Base path must be under input root: " + realInputRoot);
            }

            if (!Files.isDirectory(basePath)) {
                throw new IllegalArgumentException("Base path must be an existing directory: " + requestedBasePath);
            }

            return basePath;
        } catch (IOException e) {
            throw new IllegalArgumentException("Base path does not exist or cannot be accessed: " + requestedBasePath);
        }
    }

    private String toAlphabeticName(int number) {
        StringBuilder result = new StringBuilder();

        while (number > 0) {
            number--;
            result.insert(0, (char) ('a' + number % 26));
            number /= 26;
        }

        return result.toString();
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "txt";
        }

        String normalizedExtension = extension.trim().replaceFirst("^\\.", "");

        if (normalizedExtension.contains("/") || normalizedExtension.contains("\\")) {
            throw new IllegalArgumentException("Extension must not contain path separators.");
        }

        return normalizedExtension;
    }
}