package com.codecool.backend.repository;

import com.codecool.backend.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
}