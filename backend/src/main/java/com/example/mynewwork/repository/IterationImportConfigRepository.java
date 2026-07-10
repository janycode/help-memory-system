package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.IterationImportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IterationImportConfigRepository extends JpaRepository<IterationImportConfig, Long> {

    Optional<IterationImportConfig> findByActiveTrue();

    Optional<IterationImportConfig> findTopByOrderByCreatedAtDesc();
}
