package com.codecool.backend.dto;

public record GenerateResponse(
        String basePath,
        int depth,
        int createdDirectories,
        int createdFiles,
        String deepestPath
) {
}