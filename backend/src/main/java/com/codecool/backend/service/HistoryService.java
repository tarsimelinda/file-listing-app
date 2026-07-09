package com.codecool.backend.service;

import com.codecool.backend.entity.QueryHistory;
import com.codecool.backend.repository.QueryHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryService {

    private final QueryHistoryRepository queryHistoryRepository;
    private final RunningUserProvider runningUserProvider;

    public HistoryService(
            QueryHistoryRepository queryHistoryRepository,
            RunningUserProvider runningUserProvider
    ) {
        this.queryHistoryRepository = queryHistoryRepository;
        this.runningUserProvider = runningUserProvider;
    }

    public QueryHistory saveHistory(
            String requestedPath,
            String extension,
            int resultCount,
            String status
    ) {
        QueryHistory history = new QueryHistory(
                runningUserProvider.getRunUser(),
                runningUserProvider.getRunUid(),
                runningUserProvider.getRunGid(),
                requestedPath,
                extension,
                LocalDateTime.now(),
                resultCount,
                status
        );

        return queryHistoryRepository.save(history);
    }

    public List<QueryHistory> getHistory() {
        return queryHistoryRepository.findAll();
    }
}