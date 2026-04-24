package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 字典数据实体
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "sys_dict_data", indexes = {
    @Index(name = "idx_type_code", columnList = "type_code")
})
@EntityListeners(AuditingEntityListener.class)
public class SysDictData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", nullable = false, length = 100)
    private String typeCode;

    @Column(name = "data_value", nullable = false, length = 100)
    private String dataValue;

    @Column(name = "data_label", nullable = false, length = 200)
    private String dataLabel;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(length = 500)
    private String remark;

    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
