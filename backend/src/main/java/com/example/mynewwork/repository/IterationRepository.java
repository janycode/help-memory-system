package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.Iteration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IterationRepository extends JpaRepository<Iteration, Long> {

    List<Iteration> findByActiveTrue();

    Optional<Iteration> findByIdAndActiveTrue(Long id);

    Optional<Iteration> findByIssueNumberAndProjectCodeAndActiveTrue(String issueNumber, String projectCode);

    Optional<Iteration> findFirstByIssueNumberAndActiveTrue(String issueNumber);

    @Query("SELECT i FROM Iteration i WHERE i.active = true AND (i.issueNumber LIKE %:keyword% OR i.projectCode LIKE %:keyword% OR i.title LIKE %:keyword%)")
    List<Iteration> findActiveByKeyword(@Param("keyword") String keyword);

    List<Iteration> findByStatusAndActiveTrue(String status);

    List<Iteration> findByPriorityAndActiveTrue(String priority);

    long countByActiveTrue();

    Page<Iteration> findByActiveTrue(Pageable pageable);

    Page<Iteration> findByStatusAndActiveTrue(String status, Pageable pageable);

    Page<Iteration> findByPriorityAndActiveTrue(String priority, Pageable pageable);
}