package com.codecool.backend.dto;

public record ScanRequest(
        String basePath,
        String signature,
        String extension
) {
}