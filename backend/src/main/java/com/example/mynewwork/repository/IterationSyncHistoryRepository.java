package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.IterationSyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IterationSyncHistoryRepository extends JpaRepository<IterationSyncHistory, Long> {

    List<IterationSyncHistory> findByIterationIdOrderBySyncedAtDesc(Long iterationId);

    List<IterationSyncHistory> findTop100ByIterationIdOrderBySyncedAtDesc(Long iterationId);
}
