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
    private FileListingService service;

    @BeforeEach
    void setUp() throws IOException {
        inputRoot = tempDirectory.resolve("input");
        Files.createDirectories(inputRoot);

        service = new FileListingService(inputRoot.toString());
    }

    @Test
    void listFilesShouldReturnAllMatchingFilesRecursively() throws IOException {
        Path firstDirectory = inputRoot.resolve("a");
        Path secondDirectory = firstDirectory.resolve("b");

        Files.createDirectories(secondDirectory);

        Files.writeString(inputRoot.resolve("root.txt"), "content");
        Files.writeString(firstDirectory.resolve("first.txt"), "content");
        Files.writeString(secondDirectory.resolve("second.txt"), "content");
        Files.writeString(secondDirectory.resolve("ignored.json"), "content");

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of(
                        "a/b/second.txt",
                        "a/first.txt",
                        "root.txt"
                ),
                result
        );
    }

    @Test
    void listFilesShouldReturnAllFilesWhenExtensionIsNull() throws IOException {
        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");
        Files.writeString(inputRoot.resolve("third.csv"), "content");

        List<String> result = service.listFiles(
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
    void listFilesShouldReturnAllFilesWhenExtensionIsBlank() throws IOException {
        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");

        List<String> result = service.listFiles(
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
    void listFilesShouldRemoveLeadingDotFromExtension() throws IOException {
        Files.writeString(inputRoot.resolve("first.txt"), "content");
        Files.writeString(inputRoot.resolve("second.json"), "content");

        List<String> result = service.listFiles(
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

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "  txt  "
        );

        assertEquals(
                List.of("first.txt"),
                result
        );
    }

    @Test
    void listFilesShouldReturnRelativePaths() throws IOException {
        Path nestedDirectory = inputRoot
                .resolve("first")
                .resolve("second");

        Files.createDirectories(nestedDirectory);
        Files.writeString(
                nestedDirectory.resolve("file.txt"),
                "content"
        );

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("first/second/file.txt"),
                result
        );
    }

    @Test
    void listFilesShouldReturnSortedPaths() throws IOException {
        Files.writeString(inputRoot.resolve("z.txt"), "content");
        Files.writeString(inputRoot.resolve("a.txt"), "content");
        Files.writeString(inputRoot.resolve("m.txt"), "content");

        List<String> result = service.listFiles(
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
    void listFilesShouldReturnEmptyListWhenNoFilesMatch() throws IOException {
        Files.writeString(inputRoot.resolve("first.json"), "content");
        Files.writeString(inputRoot.resolve("second.csv"), "content");

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void listFilesShouldReturnEmptyListForEmptyDirectory() {
        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void listFilesShouldNotIncludeDirectories() throws IOException {
        Files.createDirectories(inputRoot.resolve("directory.txt"));
        Files.writeString(inputRoot.resolve("file.txt"), "content");

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("file.txt"),
                result
        );
    }

    @Test
    void listFilesShouldUseRequestedSubdirectoryAsRelativePathRoot()
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
                inputRoot.resolve("outside-requested-directory.txt"),
                "content"
        );

        List<String> result = service.listFiles(
                requestedDirectory.toString(),
                "txt"
        );

        assertEquals(
                List.of(
                        "first.txt",
                        "nested/second.txt"
                ),
                result
        );
    }

    @Test
    void listFilesShouldRejectNullPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.listFiles(null, "txt")
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
                () -> service.listFiles(" ", "txt")
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
                () -> service.listFiles(
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
                () -> service.listFiles(
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
                () -> service.listFiles(
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
                () -> service.listFiles(
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
                () -> service.listFiles(
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

        Files.writeString(inputRoot.resolve("lowercase.txt"), "content");
        Files.writeString(inputRoot.resolve("uppercase.TXT"), "content");

        List<String> result = service.listFiles(
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
        Files.writeString(inputRoot.resolve("also-incorrect.atxt"), "content");

        List<String> result = service.listFiles(
                inputRoot.toString(),
                "txt"
        );

        assertEquals(
                List.of("correct.txt"),
                result
        );
    }
}