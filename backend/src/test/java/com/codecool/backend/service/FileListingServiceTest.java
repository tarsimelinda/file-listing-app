package com.codecool.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileListingServiceTest {

    @TempDir
    Path tempDirectory;

    private Path inputRoot;
    private FileListingService fileListingService;

    @BeforeEach
    void setUp() throws IOException {
        inputRoot = tempDirectory.resolve("input");
        Files.createDirectories(inputRoot);

        fileListingService = new FileListingService(inputRoot.toString());
    }

    @Test
    void listFilesShouldReturnMatchingFileNamesRecursively() throws IOException {
        Path firstDirectory = inputRoot.resolve("a");
        Path secondDirectory = firstDirectory.resolve("b");

        Files.createDirectories(secondDirectory);

        Files.writeString(inputRoot.resolve("root.txt"), "content");
        Files.writeString(firstDirectory.resolve("first.txt"), "content");
        Files.writeString(secondDirectory.resolve("second.txt"), "content");
        Files.writeString(secondDirectory.resolve("ignored.json"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of(
                        "first.txt",
                        "root.txt",
                        "second.txt"
                ),
                result
        );
    }

    @Test
    void listFilesShouldReturnOnlyUniqueFileNames() throws IOException {
        Path firstDirectory = inputRoot.resolve("first");
        Path secondDirectory = inputRoot.resolve("second");

        Files.createDirectories(firstDirectory);
        Files.createDirectories(secondDirectory);

        Files.writeString(
                firstDirectory.resolve("duplicate.txt"),
                "first content"
        );

        Files.writeString(
                secondDirectory.resolve("duplicate.txt"),
                "second content"
        );

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("duplicate.txt"),
                result
        );
    }

    @Test
    void listFilesShouldReturnOnlyFileNamesWithoutDirectoryPaths()
            throws IOException {

        Path nestedDirectory = inputRoot
                .resolve("first")
                .resolve("second");

        Files.createDirectories(nestedDirectory);
        Files.writeString(
                nestedDirectory.resolve("file.txt"),
                "content"
        );

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("file.txt"),
                result
        );
    }

    @Test
    void listFilesShouldReturnAllFilesWhenExtensionIsNull()
            throws IOException {

        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");
        Files.writeString(inputRoot.resolve("third.csv"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                null
        );

        assertEquals(
                List.of(
                        "first.txt",
                        "second.json",
                        "third.csv"
                ),
                result
        );
    }

    @Test
    void listFilesShouldReturnAllFilesWhenExtensionIsBlank()
            throws IOException {

        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                " "
        );

        assertEquals(
                List.of(
                        "first.txt",
                        "second.json"
                ),
                result
        );
    }

    @Test
    void listFilesShouldRemoveLeadingDotFromExtension()
            throws IOException {

        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                ".txt"
        );

        assertEquals(
                List.of("first.txt"),
                result
        );
    }

    @Test
    void listFilesShouldTrimExtension() throws IOException {
        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "  txt  "
        );

        assertEquals(
                List.of("first.txt"),
                result
        );
    }

    @Test
    void listFilesShouldReturnSortedFileNames() throws IOException {
        Files.writeString(inputRoot.resolve("z.txt"), "content");
        Files.writeString(inputRoot.resolve("a.txt"), "content");
        Files.writeString(inputRoot.resolve("m.txt"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of(
                        "a.txt",
                        "m.txt",
                        "z.txt"
                ),
                result
        );
    }

    @Test
    void listFilesShouldReturnEmptyListWhenNoFilesMatch()
            throws IOException {

        Files.writeString(inputRoot.resolve("first.json"), "content");
        Files.writeString(inputRoot.resolve("second.csv"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void listFilesShouldReturnEmptyListForEmptyDirectory() {
        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listFilesShouldNotIncludeDirectories() throws IOException {
        Files.createDirectories(inputRoot.resolve("directory.txt"));
        Files.writeString(inputRoot.resolve("file.txt"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("file.txt"),
                result
        );
    }

    @Test
    void listFilesShouldSearchOnlyUnderRequestedSubdirectory()
            throws IOException {

        Path requestedDirectory = inputRoot.resolve("requested");
        Path nestedDirectory = requestedDirectory.resolve("nested");

        Files.createDirectories(nestedDirectory);

        Files.writeString(
                requestedDirectory.resolve("first.txt"),
                "content"
        );

        Files.writeString(
                nestedDirectory.resolve("second.txt"),
                "content"
        );

        Files.writeString(
                inputRoot.resolve("outside.txt"),
                "content"
        );

        List<String> result = fileListingService.listFiles(
                requestedDirectory.toString(),
                "txt"
        );

        assertEquals(
                List.of(
                        "first.txt",
                        "second.txt"
                ),
                result
        );
    }

    @Test
    void listFilesShouldRejectNullPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(null, "txt")
        );

        assertEquals(
                "Path must not be empty.",
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldRejectBlankPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(" ", "txt")
        );

        assertEquals(
                "Path must not be empty.",
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldRejectNonExistingPath() {
        Path missingPath = inputRoot.resolve("missing");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(
                        missingPath.toString(),
                        "txt"
                )
        );

        assertEquals(
                "Path does not exist or cannot be accessed: " + missingPath,
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldRejectPathThatIsNotDirectory()
            throws IOException {

        Path filePath = inputRoot.resolve("file.txt");
        Files.writeString(filePath, "content");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(
                        filePath.toString(),
                        "txt"
                )
        );

        assertEquals(
                "Path is not a directory: " + filePath,
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldRejectPathOutsideInputRoot()
            throws IOException {

        Path outsideDirectory = tempDirectory.resolve("outside");
        Files.createDirectories(outsideDirectory);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(
                        outsideDirectory.toString(),
                        "txt"
                )
        );

        assertTrue(
                exception.getMessage()
                        .startsWith("Path must be under input root:")
        );
    }

    @Test
    void listFilesShouldRejectExtensionContainingForwardSlash() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(
                        inputRoot.toString(),
                        "folder/txt"
                )
        );

        assertEquals(
                "Extension must not contain path separators.",
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldRejectExtensionContainingBackslash() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileListingService.listFiles(
                        inputRoot.toString(),
                        "folder\\txt"
                )
        );

        assertEquals(
                "Extension must not contain path separators.",
                exception.getMessage()
        );
    }

    @Test
    void listFilesShouldMatchExtensionCaseSensitively()
            throws IOException {

        Files.writeString(
                inputRoot.resolve("lowercase.txt"),
                "content"
        );

        Files.writeString(
                inputRoot.resolve("uppercase.TXT"),
                "content"
        );

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("lowercase.txt"),
                result
        );
    }

    @Test
    void listFilesShouldOnlyMatchCompleteExtension()
            throws IOException {

        Files.writeString(inputRoot.resolve("correct.txt"), "content");
        Files.writeString(inputRoot.resolve("incorrecttxt"), "content");
        Files.writeString(inputRoot.resolve("incorrect.atxt"), "content");

        List<String> result = fileListingService.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("correct.txt"),
                result
        );
    }
}