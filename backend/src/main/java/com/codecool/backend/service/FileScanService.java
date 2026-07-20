package com.codecool.backend.service;

import com.codecool.backend.dto.ScanMatch;
import com.codecool.backend.dto.ScanRequest;
import com.codecool.backend.dto.ScanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileScanService {

    private static final int BUFFER_SIZE = 64 * 1024;

    private final Path inputRoot;

    public FileScanService(@Value("${app.input-root}") String inputRoot) {
        this.inputRoot = Path.of(inputRoot).toAbsolutePath().normalize();
    }

    public ScanResponse scan(ScanRequest request) {
        validateRequest(request);

        Path basePath = resolvePath(request.basePath());
        String extension = normalizeExtension(request.extension());
        byte[] signature = request.signature().getBytes(StandardCharsets.UTF_8);
        int[] prefixTable = buildPrefixTable(signature);

        int scannedFiles = 0;
        List<ScanMatch> matches = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(basePath)) {
            Iterator<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesExtension(path, extension))
                    .iterator();

            while (files.hasNext()) {
                Path file = files.next();
                scannedFiles++;

                if (containsSignature(file, signature, prefixTable)) {
                    matches.add(new ScanMatch(
                            file.getFileName().toString(),
                            basePath.relativize(file).toString().replace("\\", "/")
                    ));
                }
            }

            return new ScanResponse(
                    request.basePath(),
                    request.signature(),
                    extension,
                    scannedFiles,
                    matches.size(),
                    matches
            );
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("Failed to scan files under: " + request.basePath(), e);
        }
    }

    private boolean containsSignature(Path file, byte[] signature, int[] prefixTable) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int matched = 0;

        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    while (matched > 0 && buffer[i] != signature[matched]) {
                        matched = prefixTable[matched - 1];
                    }

                    if (buffer[i] == signature[matched]) {
                        matched++;
                    }

                    if (matched == signature.length) {
                        return true;
                    }
                }
            }

            return false;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + file, e);
        }
    }

    private int[] buildPrefixTable(byte[] signature) {
        int[] prefixTable = new int[signature.length];
        int matched = 0;

        for (int i = 1; i < signature.length; i++) {
            while (matched > 0 && signature[i] != signature[matched]) {
                matched = prefixTable[matched - 1];
            }

            if (signature[i] == signature[matched]) {
                matched++;
                prefixTable[i] = matched;
            }
        }

        return prefixTable;
    }

    private void validateRequest(ScanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Scan request must not be empty.");
        }

        if (request.basePath() == null || request.basePath().isBlank()) {
            throw new IllegalArgumentException("Base path must not be empty.");
        }

        if (request.signature() == null || request.signature().isBlank()) {
            throw new IllegalArgumentException("Signature must not be empty.");
        }
    }

    private Path resolvePath(String requestedPath) {
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
        return path.getFileName().toString().endsWith("." + extension);
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
