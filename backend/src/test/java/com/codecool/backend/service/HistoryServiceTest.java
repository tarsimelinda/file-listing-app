package com.codecool.backend.service;

import com.codecool.backend.entity.QueryHistory;
import com.codecool.backend.repository.QueryHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @Mock
    private RunningUserProvider runningUserProvider;

    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new HistoryService(
                queryHistoryRepository,
                runningUserProvider
        );
    }

    @Test
    void saveHistoryShouldCreateAndSaveQueryHistory() {
        when(runningUserProvider.getRunUser())
                .thenReturn("test-user");
        when(runningUserProvider.getRunUid())
                .thenReturn("1000");
        when(runningUserProvider.getRunGid())
                .thenReturn("1000");

        when(queryHistoryRepository.save(any(QueryHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeSave = LocalDateTime.now();

        QueryHistory result = historyService.saveHistory(
                "/input/generated",
                "txt",
                5,
                "SUCCESS"
        );

        LocalDateTime afterSave = LocalDateTime.now();

        ArgumentCaptor<QueryHistory> historyCaptor =
                ArgumentCaptor.forClass(QueryHistory.class);

        verify(queryHistoryRepository).save(historyCaptor.capture());

        QueryHistory savedHistory = historyCaptor.getValue();

        assertAll(
                () -> assertSame(savedHistory, result),

                () -> assertEquals(
                        "test-user",
                        savedHistory.getRunUser()
                ),
                () -> assertEquals(
                        "1000",
                        savedHistory.getRunUid()
                ),
                () -> assertEquals(
                        "1000",
                        savedHistory.getRunGid()
                ),
                () -> assertEquals(
                        "/input/generated",
                        savedHistory.getRequestedPath()
                ),
                () -> assertEquals(
                        "txt",
                        savedHistory.getExtension()
                ),
                () -> assertEquals(
                        5,
                        savedHistory.getResultCount()
                ),
                () -> assertEquals(
                        "SUCCESS",
                        savedHistory.getStatus()
                ),

                () -> assertNotNull(savedHistory.getRequestedAt()),
                () -> assertFalse(
                        savedHistory.getRequestedAt()
                                .isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedHistory.getRequestedAt()
                                .isAfter(afterSave)
                )
        );
    }

    @Test
    void saveHistoryShouldRequestRunningUserInformation() {
        when(queryHistoryRepository.save(any(QueryHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        historyService.saveHistory(
                "/input",
                "json",
                2,
                "SUCCESS"
        );

        verify(runningUserProvider).getRunUser();
        verify(runningUserProvider).getRunUid();
        verify(runningUserProvider).getRunGid();
    }

    @Test
    void saveHistoryShouldReturnEntityReturnedByRepository() {
        QueryHistory repositoryResult = mock(QueryHistory.class);

        when(queryHistoryRepository.save(any(QueryHistory.class)))
                .thenReturn(repositoryResult);

        QueryHistory result = historyService.saveHistory(
                "/input",
                "txt",
                3,
                "SUCCESS"
        );

        assertSame(repositoryResult, result);
    }

    @Test
    void getHistoryShouldReturnAllHistoryEntries() {
        QueryHistory firstHistory = mock(QueryHistory.class);
        QueryHistory secondHistory = mock(QueryHistory.class);

        List<QueryHistory> expectedHistory = List.of(
                firstHistory,
                secondHistory
        );

        when(queryHistoryRepository.findAll())
                .thenReturn(expectedHistory);

        List<QueryHistory> result = historyService.getHistory();

        assertSame(expectedHistory, result);
        verify(queryHistoryRepository).findAll();
    }

    @Test
    void getHistoryShouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(queryHistoryRepository.findAll())
                .thenReturn(List.of());

        List<QueryHistory> result = historyService.getHistory();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(queryHistoryRepository).findAll();
    }

    @Test
    void getHistoryShouldNotSaveOrModifyHistory() {
        historyService.getHistory();

        verify(queryHistoryRepository).findAll();
        verify(queryHistoryRepository, never())
                .save(any(QueryHistory.class));

        verifyNoInteractions(runningUserProvider);
    }
}