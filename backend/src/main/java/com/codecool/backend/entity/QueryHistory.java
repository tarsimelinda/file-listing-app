package com.codecool.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String runUser;
    private String runUid;
    private String runGid;
    private String requestedPath;
    private String extension;
    private LocalDateTime requestedAt;
    private Integer resultCount;
    private String status;

    public QueryHistory() {
    }

    public QueryHistory(
            String runUser,
            String runUid,
            String runGid,
            String requestedPath,
            String extension,
            LocalDateTime requestedAt,
            Integer resultCount,
            String status
    ) {
        this.runUser = runUser;
        this.runUid = runUid;
        this.runGid = runGid;
        this.requestedPath = requestedPath;
        this.extension = extension;
        this.requestedAt = requestedAt;
        this.resultCount = resultCount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getRunUser() {
        return runUser;
    }

    public String getRunUid() {
        return runUid;
    }

    public String getRunGid() {
        return runGid;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public String getExtension() {
        return extension;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public String getStatus() {
        return status;
    }
}