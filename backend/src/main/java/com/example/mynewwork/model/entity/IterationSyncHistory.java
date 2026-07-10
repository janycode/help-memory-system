package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "iteration_sync_history")
@Data
@EntityListeners(AuditingEntityListener.class)
public class IterationSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iteration_id", nullable = false)
    private Long iterationId;

    @Column(name = "sync_type", nullable = false, length = 20)
    private String syncType; // PAGE_TO_LOCAL, LOCAL_TO_PAGE

    @Column(name = "field_name", length = 50)
    private String fieldName; // developmentNotes, releaseNotes, title, etc.

    @Column(name = "old_value", columnDefinition = "LONGTEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "LONGTEXT")
    private String newValue;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
