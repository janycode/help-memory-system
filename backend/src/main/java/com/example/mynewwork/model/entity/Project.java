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
 * 项目信息实体
 *
 * 管理项目代码仓库、文档位置、部署路径等信息
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Entity
@Table(name = "projects")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class Project {

    /**
     * 项目ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目名称
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * 项目描述
     */
    @Column
    private String description;

    /**
     * Git仓库地址
     */
    @Column
    private String codeRepository;

    /**
     * 文档位置
     */
    @Column
    private String documentPath;

    /**
     * 部署路径
     */
    @Column
    private String deploymentPath;

    /**
     * 端口号
     */
    @Column
    private String port;

    /**
     * 团队成员（JSON格式）
     */
    @Column
    private String teamMembers;

    /**
     * 项目全称
     */
    @Column
    private String projectFullName;

    @Column
    private String status;

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
     * 备注信息
     */
    @Column(length = 1000)
    private String notes;

    /**
     * 是否启用
     */
    @Column
    private Boolean active = true;
}
