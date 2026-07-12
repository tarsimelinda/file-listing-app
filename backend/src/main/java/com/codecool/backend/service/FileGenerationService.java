package com.codecool.backend.service;

import com.codecool.backend.dto.GenerateRequest;
import com.codecool.backend.dto.GenerateResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileGenerationService {

    public GenerateResponse generate(GenerateRequest request) {
        if (request.depth() <= 0) {
            throw new IllegalArgumentException("Depth must be greater than zero.");
        }

        if (request.filesPerDirectory() < 0) {
            throw new IllegalArgumentException("Files per directory cannot be negative.");
        }

        String extension = normalizeExtension(request.extension());

        Path basePath = Path.of(request.basePath()).normalize();
        Path generatedRoot = basePath.resolve("generated");

        int createdDirectories = 0;
        int createdFiles = 0;

        try {
            Files.createDirectories(generatedRoot);

            Path currentPath = generatedRoot;

            for (int level = 1; level <= request.depth(); level++) {
                String directoryName = toAlphabeticName(level);

                currentPath = currentPath.resolve(directoryName);
                Files.createDirectories(currentPath);
                createdDirectories++;

                for (int fileNumber = 1; fileNumber <= request.filesPerDirectory(); fileNumber++) {
                    String fileName = level + "." + extension;
                    Path filePath = currentPath.resolve(fileName);

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