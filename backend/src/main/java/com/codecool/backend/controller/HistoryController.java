package com.codecool.backend.controller;

import com.codecool.backend.entity.QueryHistory;
import com.codecool.backend.service.HistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/api/history")
    public List<QueryHistory> getHistory() {
        return historyService.getHistory();
    }

}
