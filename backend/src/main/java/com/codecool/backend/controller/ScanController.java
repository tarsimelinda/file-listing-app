package com.codecool.backend.controller;

import com.codecool.backend.dto.ScanRequest;
import com.codecool.backend.dto.ScanResponse;
import com.codecool.backend.service.FileScanService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final FileScanService fileScanService;

    public ScanController(FileScanService fileScanService) {
        this.fileScanService = fileScanService;
    }

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        return fileScanService.scan(request);
    }
}