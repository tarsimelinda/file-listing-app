package com.codecool.backend.service;

import com.codecool.backend.dto.GenerateRequest;
import com.codecool.backend.dto.GenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileGenerationServiceTest {

    @TempDir
    Path tempDirectory;

    private Path inputRoot;
    private FileGenerationService service;

    @BeforeEach
    void setUp() throws IOException {
        inputRoot = tempDirectory.resolve("input");
        Files.createDirectories(inputRoot);

        service = new FileGenerationService(inputRoot.toString());
    }

    @Test
    void generateShouldCreateRequestedDirectoryStructureAndFiles() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                3,
                2,
                "txt"
        );

        GenerateResponse response = service.generate(request);

        Path generatedRoot = inputRoot.resolve("generated");
        Path firstLevel = generatedRoot.resolve("a");
        Path secondLevel = firstLevel.resolve("b");
        Path thirdLevel = secondLevel.resolve("c");

        assertAll(
                () -> assertTrue(Files.isDirectory(generatedRoot)),
                () -> assertTrue(Files.isDirectory(firstLevel)),
                () -> assertTrue(Files.isDirectory(secondLevel)),
                () -> assertTrue(Files.isDirectory(thirdLevel)),

                () -> assertTrue(Files.exists(firstLevel.resolve("1.txt"))),
                () -> assertTrue(Files.exists(firstLevel.resolve("2.txt"))),

                () -> assertTrue(Files.exists(secondLevel.resolve("1.txt"))),
                () -> assertTrue(Files.exists(secondLevel.resolve("2.txt"))),

                () -> assertTrue(Files.exists(thirdLevel.resolve("1.txt"))),
                () -> assertTrue(Files.exists(thirdLevel.resolve("2.txt"))),

                () -> assertEquals(3, response.createdDirectories()),
                () -> assertEquals(6, response.createdFiles()),
                () -> assertEquals(3, response.depth()),
                () -> assertEquals("a/b/c", response.deepestPath())
        );
    }

    @Test
    void generateShouldWriteExpectedFileContent() throws IOException {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                "log"
        );

        service.generate(request);

        Path generatedFile = inputRoot
                .resolve("generated")
                .resolve("a")
                .resolve("1.log");

        assertEquals(
                "Generated file: 1.log",
                Files.readString(generatedFile)
        );
    }

    @Test
    void generateShouldUseTxtExtensionWhenExtensionIsBlank() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                " "
        );

        service.generate(request);

        Path generatedFile = inputRoot
                .resolve("generated")
                .resolve("a")
                .resolve("1.txt");

        assertTrue(Files.exists(generatedFile));
    }

    @Test
    void generateShouldRemoveLeadingDotFromExtension() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                ".json"
        );

        service.generate(request);

        Path generatedFile = inputRoot
                .resolve("generated")
                .resolve("a")
                .resolve("1.json");

        assertTrue(Files.exists(generatedFile));
        assertFalse(
                Files.exists(
                        inputRoot.resolve("generated")
                                .resolve("a")
                                .resolve("1..json")
                )
        );
    }

    @Test
    void generateShouldDeletePreviouslyGeneratedDirectory() throws IOException {
        Path oldGeneratedDirectory = inputRoot
                .resolve("generated")
                .resolve("old-directory");

        Files.createDirectories(oldGeneratedDirectory);
        Files.writeString(
                oldGeneratedDirectory.resolve("old-file.txt"),
                "Old content"
        );

        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                "txt"
        );

        service.generate(request);

        assertAll(
                () -> assertFalse(Files.exists(oldGeneratedDirectory)),
                () -> assertTrue(
                        Files.exists(
                                inputRoot.resolve("generated")
                                        .resolve("a")
                                        .resolve("1.txt")
                        )
                )
        );
    }

    @Test
    void generateShouldCreateAlphabeticDirectoryNamesAfterZ() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                27,
                0,
                "txt"
        );

        GenerateResponse response = service.generate(request);

        Path level26 = buildPathUntil(inputRoot.resolve("generated"), 26);
        Path level27 = level26.resolve("aa");

        assertAll(
                () -> assertTrue(Files.isDirectory(level26)),
                () -> assertTrue(Files.isDirectory(level27)),
                () -> assertTrue(response.deepestPath().endsWith("/z/aa"))
        );
    }

    @Test
    void generateShouldRejectNullBasePath() {
        GenerateRequest request = new GenerateRequest(
                null,
                1,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Base path must not be empty.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectBlankBasePath() {
        GenerateRequest request = new GenerateRequest(
                " ",
                1,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Base path must not be empty.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectZeroDepth() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                0,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Depth must be greater than zero.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectDepthGreaterThanMaximum() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                101,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Depth must not be greater than 100.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectNegativeFilesPerDirectory() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                -1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Files per directory cannot be negative.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectTooManyFilesPerDirectory() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                21,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Files per directory must not be greater than 20.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectBasePathOutsideInputRoot() throws IOException {
        Path outsideDirectory = tempDirectory.resolve("outside");
        Files.createDirectories(outsideDirectory);

        GenerateRequest request = new GenerateRequest(
                outsideDirectory.toString(),
                1,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertTrue(
                exception.getMessage()
                        .startsWith("Base path must be under input root:")
        );
    }

    @Test
    void generateShouldRejectNonExistingBasePath() {
        Path missingDirectory = inputRoot.resolve("missing");

        GenerateRequest request = new GenerateRequest(
                missingDirectory.toString(),
                1,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Base path does not exist: " + missingDirectory,
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectBasePathThatIsNotDirectory() throws IOException {
        Path file = inputRoot.resolve("file.txt");
        Files.writeString(file, "content");

        GenerateRequest request = new GenerateRequest(
                file.toString(),
                1,
                1,
                "txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Base path is not a directory: " + file,
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectExtensionContainingForwardSlash() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                "folder/txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Extension must not contain path separators.",
                exception.getMessage()
        );
    }

    @Test
    void generateShouldRejectExtensionContainingBackslash() {
        GenerateRequest request = new GenerateRequest(
                inputRoot.toString(),
                1,
                1,
                "folder\\txt"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request)
        );

        assertEquals(
                "Extension must not contain path separators.",
                exception.getMessage()
        );
    }

    private Path buildPathUntil(Path startingPath, int depth) {
        Path currentPath = startingPath;

        for (int level = 1; level <= depth; level++) {
            currentPath = currentPath.resolve(toAlphabeticName(level));
        }

        return currentPath;
    }

    private String toAlphabeticName(int number) {
        StringBuilder result = new StringBuilder();

        while (number > 0) {
            number--;

            char letter = (char) ('a' + number % 26);
            result.insert(0, letter);

            number /= 26;
        }

        return result.toString();
    }
}