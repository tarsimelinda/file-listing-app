package com.codecool.backend.dto;

public record GenerateRequest(
        String basePath,
        int depth,
        int filesPerDirectory,
        String extension
) {
}