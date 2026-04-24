package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 业务流程实体
 *
 * 管理发布流程、业务流程、运维流程等文档和检查清单
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Entity
@Table(name = "business_processes")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class BusinessProcess {

    /**
     * 流程ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程名称
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * 流程描述
     */
    @Column
    private String description;

    /**
     * 流程分类（发布流程/业务流程/运维流程等）
     */
    @Column(nullable = false)
    private String category;

    /**
     * 关联环境ID（为空表示不区分环境）
     */
    @Column(name = "environment_id")
    private Long environmentId;

    /**
     * 环境类型（关联字典 environment_type，为空表示不区分环境）
     */
    @Column(name = "environment_type")
    private String environmentType;

    /**
     * 流程详细描述
     */
    @Column(columnDefinition = "TEXT")
    private String processFlow;

    /**
     * 流程图文件路径
     */
    @Column
    private String flowchartPath;

    /**
     * 操作步骤（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String steps;

    /**
     * 注意事项
     */
    @Column(columnDefinition = "TEXT")
    private String precautions;

    /**
     * 检查清单（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String checklist;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 相关文档链接（JSON格式）
     */
    @Column(length = 1000)
    private String relatedDocuments;

    /**
     * 优先级（数值越大优先级越高）
     */
    @Column
    private Integer priority;

    /**
     * 是否启用
     */
    @Column
    private Boolean active = true;
}
