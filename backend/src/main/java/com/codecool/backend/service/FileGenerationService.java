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

    private final Path inputRoot;

    public FileGenerationService(@Value("${app.input-root}") String inputRoot) {
        this.inputRoot = Path.of(inputRoot).toAbsolutePath().normalize();
    }

    public GenerateResponse generate(GenerateRequest request) {
        validateRequest(request);

        String extension = normalizeExtension(request.extension());

        Path basePath = resolveAndValidateBasePath(request.basePath());
        Path generatedRoot = basePath.resolve("generated").normalize();

        int createdDirectories = 0;
        int createdFiles = 0;

        try {
            deleteDirectoryIfExists(generatedRoot);
            Files.createDirectories(generatedRoot);

            Path currentPath = generatedRoot;

            for (int level = 1; level <= request.depth(); level++) {
                String directoryName = toAlphabeticName(level);

                currentPath = currentPath.resolve(directoryName).normalize();

                if (!Files.exists(currentPath)) {
                    Files.createDirectories(currentPath);
                    createdDirectories++;
                }

                for (int fileNumber = 1; fileNumber <= request.filesPerDirectory(); fileNumber++) {
                    String fileName = fileNumber + "." + extension;
                    Path filePath = currentPath.resolve(fileName).normalize();

                    Files.writeString(filePath, "Generated file: " + fileName);
                    createdFiles++;
                }
            }

            return new GenerateResponse(
                    request.basePath(),
                    request.depth(),
                    createdDirectories,
                    createdFiles,
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
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to delete path: " + path, e);
                        }
                    });
        }
    }

    private void validateRequest(GenerateRequest request) {
        if (request.basePath() == null || request.basePath().isBlank()) {
            throw new IllegalArgumentException("Base path must not be empty.");
        }

        if (request.depth() <= 0) {
            throw new IllegalArgumentException("Depth must be greater than zero.");
        }

        if (request.filesPerDirectory() < 0) {
            throw new IllegalArgumentException("Files per directory cannot be negative.");
        }
    }

    private Path resolveAndValidateBasePath(String requestedBasePath) {
        Path resolvedBasePath = Path.of(requestedBasePath).toAbsolutePath().normalize();

        if (!resolvedBasePath.startsWith(inputRoot)) {
            throw new IllegalArgumentException("Base path must be under input root: " + inputRoot);
        }

        return resolvedBasePath;
    }

    private String toAlphabeticName(int number) {
        StringBuilder result = new StringBuilder();

        while (number > 0) {
            number--;
            char letter = (char) ('a' + (number % 26));
            result.insert(0, letter);
            number /= 26;
        }

        return result.toString();
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "txt";
        }

        return extension.trim().replaceFirst("^\\.", "");
    }
}