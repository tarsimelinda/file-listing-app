package com.codecool.backend.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class RunningUserProvider {

    private static final Path PROCESS_STATUS_PATH = Path.of("/proc/self/status");

    public String getRunUser() {
        return System.getProperty("user.name", "unknown");
    }

    public String getRunUid() {
        return readProcessStatusValue("Uid");
    }

    public String getRunGid() {
        return readProcessStatusValue("Gid");
    }

    private String readProcessStatusValue(String key) {
        try (Stream<String> lines = Files.lines(PROCESS_STATUS_PATH)) {
            return lines
                    .filter(line -> line.startsWith(key + ":"))
                    .map(this::extractFirstNumber)
                    .findFirst()
                    .orElse("unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    private String extractFirstNumber(String line) {
        return line
                .replaceFirst("^[A-Za-z]+:\\s*", "")
                .trim()
                .split("\\s+")[0];
    }
}