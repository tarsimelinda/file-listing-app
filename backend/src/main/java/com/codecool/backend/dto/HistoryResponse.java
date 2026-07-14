package com.codecool.backend.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
        Long id,
        String runUser,
        String runUid,
        String runGid,
        String requestedPath,
        String extension,
        LocalDateTime requestedAt,
        Integer resultCount,
        String status
) {
}