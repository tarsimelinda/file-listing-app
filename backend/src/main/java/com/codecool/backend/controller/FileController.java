package com.codecool.backend.controller;

import com.codecool.backend.dto.FileListResponse;
import com.codecool.backend.dto.GenerateResponse;
import com.codecool.backend.service.FileGenerationService;
import com.codecool.backend.service.FileListingService;
import com.codecool.backend.service.HistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.codecool.backend.dto.GenerateRequest;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
public class FileController {

    private final FileListingService fileListingService;
    private final HistoryService historyService;
    private final FileGenerationService fileGenerationService;

    public FileController(
            FileListingService fileListingService,
            HistoryService historyService,
            FileGenerationService fileGenerationService
    ) {
        this.fileListingService = fileListingService;
        this.historyService = historyService;
        this.fileGenerationService = fileGenerationService;
    }

    @GetMapping("/api/list")
    public FileListResponse listFiles(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "") String extension
    ) {
        try {
            List<String> files = fileListingService.listFiles(path, extension);

            historyService.saveHistory(
                    path,
                    extension,
                    files.size(),
                    "SUCCESS"
            );

            return new FileListResponse(
                    path,
                    extension,
                    files,
                    files.size()
            );
        } catch (RuntimeException exception) {
            historyService.saveHistory(
                    path,
                    extension,
                    0,
                    "FAILED"
            );

            throw exception;
        }
    }

    @PostMapping("/api/generate")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        try {
            GenerateResponse response = fileGenerationService.generate(request);

            historyService.saveHistory(
                    request.basePath(),
                    request.extension(),
                    response.createdFiles(),
                    "GENERATED"
            );

            return response;
        } catch (RuntimeException exception) {
            historyService.saveHistory(
                    request.basePath(),
                    request.extension(),
                    0,
                    "FAILED"
            );

            throw exception;
        }
    }
}