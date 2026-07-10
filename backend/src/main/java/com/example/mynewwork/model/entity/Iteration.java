package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "iterations")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class Iteration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "issue_number", nullable = false, length = 50)
    private String issueNumber;

    @NotBlank
    @Column(name = "project_code", nullable = false, length = 100)
    private String projectCode;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "issue_url", length = 1000)
    private String issueUrl;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String status;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "development_notes", columnDefinition = "LONGTEXT")
    private String developmentNotes;

    @Column(name = "release_notes", columnDefinition = "LONGTEXT")
    private String releaseNotes;

    @Column(name = "flowchart_path", length = 1000)
    private String flowchartPath;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "estimated_time")
    private LocalDateTime estimatedTime;

    @Column(name = "actual_time")
    private LocalDateTime actualTime;

    @Column(name = "api_doc_url", length = 1000)
    private String apiDocUrl;

    @Column(name = "impact_scope", columnDefinition = "TEXT")
    private String impactScope;

    @Column(columnDefinition = "TEXT")
    private String todos;

    @Column(name = "created_by")
    private Long createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "local_dir_path", length = 1000)
    private String localDirPath;

    @Column(name = "has_todos")
    private Boolean hasTodos = false;

    @Column
    private Boolean active = true;
}