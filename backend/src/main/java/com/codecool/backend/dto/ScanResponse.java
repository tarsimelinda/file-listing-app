package com.codecool.backend.dto;

import java.util.List;

public record ScanResponse(
        String requestedPath,
        String signature,
        String extension,
        int scannedFiles,
        int matchedFiles,
        List<ScanMatch> matches
) {
}