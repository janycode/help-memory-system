package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.SoCreationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoCreationLogRepository extends JpaRepository<SoCreationLog, Long> {

    List<SoCreationLog> findByEnvironmentOrderByCreatedAtDesc(String environment);

    List<SoCreationLog> findByStatusAndEnvironmentOrderByCreatedAtDesc(String status, String environment);
}
