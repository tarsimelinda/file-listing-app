package com.codecool.backend.dto;

import java.util.List;

public record FileListResponse(
        String requestedPath,
        String extension,
        List<String> files,
        int count
) {
}