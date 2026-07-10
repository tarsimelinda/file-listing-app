package com.codecool.backend.controller;

import com.codecool.backend.dto.FileListResponse;
import com.codecool.backend.service.FileListingService;
import com.codecool.backend.service.HistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FileController {

    private final FileListingService fileListingService;
    private final HistoryService historyService;

    public FileController(
            FileListingService fileListingService,
            HistoryService historyService
    ) {
        this.fileListingService = fileListingService;
        this.historyService = historyService;
    }

    @GetMapping("/api/list")
    public FileListResponse listFiles(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "") String extension
    ) {
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
    }
}