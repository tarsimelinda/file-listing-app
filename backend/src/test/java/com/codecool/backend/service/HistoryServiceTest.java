package com.codecool.backend.service;

import com.codecool.backend.dto.HistoryResponse;
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
                .thenReturn("1001");

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
                        "1001",
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

                () -> assertNotNull(
                        savedHistory.getRequestedAt()
                ),

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
    void getHistoryShouldConvertEntitiesToHistoryResponses() {
        LocalDateTime firstRequestedAt =
                LocalDateTime.of(2026, 7, 14, 10, 30);

        LocalDateTime secondRequestedAt =
                LocalDateTime.of(2026, 7, 14, 11, 45);

        QueryHistory firstHistory = mock(QueryHistory.class);
        QueryHistory secondHistory = mock(QueryHistory.class);

        when(firstHistory.getId()).thenReturn(1L);
        when(firstHistory.getRunUser()).thenReturn("first-user");
        when(firstHistory.getRunUid()).thenReturn("1000");
        when(firstHistory.getRunGid()).thenReturn("1001");
        when(firstHistory.getRequestedPath()).thenReturn("/input/first");
        when(firstHistory.getExtension()).thenReturn("txt");
        when(firstHistory.getRequestedAt()).thenReturn(firstRequestedAt);
        when(firstHistory.getResultCount()).thenReturn(3);
        when(firstHistory.getStatus()).thenReturn("SUCCESS");

        when(secondHistory.getId()).thenReturn(2L);
        when(secondHistory.getRunUser()).thenReturn("second-user");
        when(secondHistory.getRunUid()).thenReturn("2000");
        when(secondHistory.getRunGid()).thenReturn("2001");
        when(secondHistory.getRequestedPath()).thenReturn("/input/second");
        when(secondHistory.getExtension()).thenReturn("json");
        when(secondHistory.getRequestedAt()).thenReturn(secondRequestedAt);
        when(secondHistory.getResultCount()).thenReturn(0);
        when(secondHistory.getStatus()).thenReturn("FAILED");

        when(queryHistoryRepository.findAll())
                .thenReturn(List.of(firstHistory, secondHistory));

        List<HistoryResponse> result = historyService.getHistory();

        assertEquals(2, result.size());

        HistoryResponse firstResponse = result.get(0);
        HistoryResponse secondResponse = result.get(1);

        assertAll(
                () -> assertEquals(
                        1L,
                        firstResponse.id()
                ),

                () -> assertEquals(
                        "first-user",
                        firstResponse.runUser()
                ),

                () -> assertEquals(
                        "1000",
                        firstResponse.runUid()
                ),

                () -> assertEquals(
                        "1001",
                        firstResponse.runGid()
                ),

                () -> assertEquals(
                        "/input/first",
                        firstResponse.requestedPath()
                ),

                () -> assertEquals(
                        "txt",
                        firstResponse.extension()
                ),

                () -> assertEquals(
                        firstRequestedAt,
                        firstResponse.requestedAt()
                ),

                () -> assertEquals(
                        3,
                        firstResponse.resultCount()
                ),

                () -> assertEquals(
                        "SUCCESS",
                        firstResponse.status()
                ),

                () -> assertEquals(
                        2L,
                        secondResponse.id()
                ),

                () -> assertEquals(
                        "second-user",
                        secondResponse.runUser()
                ),

                () -> assertEquals(
                        "2000",
                        secondResponse.runUid()
                ),

                () -> assertEquals(
                        "2001",
                        secondResponse.runGid()
                ),

                () -> assertEquals(
                        "/input/second",
                        secondResponse.requestedPath()
                ),

                () -> assertEquals(
                        "json",
                        secondResponse.extension()
                ),

                () -> assertEquals(
                        secondRequestedAt,
                        secondResponse.requestedAt()
                ),

                () -> assertEquals(
                        0,
                        secondResponse.resultCount()
                ),

                () -> assertEquals(
                        "FAILED",
                        secondResponse.status()
                )
        );

        verify(queryHistoryRepository).findAll();
    }

    @Test
    void getHistoryShouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(queryHistoryRepository.findAll())
                .thenReturn(List.of());

        List<HistoryResponse> result = historyService.getHistory();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(queryHistoryRepository).findAll();
    }

    @Test
    void getHistoryShouldNotInteractWithRunningUserProvider() {
        when(queryHistoryRepository.findAll())
                .thenReturn(List.of());

        historyService.getHistory();

        verify(queryHistoryRepository).findAll();
        verifyNoInteractions(runningUserProvider);
    }

    @Test
    void getHistoryShouldNotSaveAnything() {
        when(queryHistoryRepository.findAll())
                .thenReturn(List.of());

        historyService.getHistory();

        verify(queryHistoryRepository, never())
                .save(any(QueryHistory.class));
    }
}