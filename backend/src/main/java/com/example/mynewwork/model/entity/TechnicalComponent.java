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
 * 技术组件实体
 *
 * 管理数据库、缓存、消息队列等技术组件配置信息
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Entity
@Table(name = "technical_components")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class TechnicalComponent {

    /**
     * 组件ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 组件名称
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * 组件描述
     */
    @Column
    private String description;

    /**
     * 组件分类（Database/Cache/MessageQueue/API等）
     */
    @Column(nullable = false)
    private String category;

    /**
     * 组件访问地址
     */
    @Column(columnDefinition = "TEXT")
    private String url;

    /**
     * 访问用户名
     */
    @Column
    private String username;

    /**
     * 访问密码（加密存储）
     */
    @Column
    private String password;

    /**
     * 组件版本
     */
    @Column
    private String version;


    /**
     * 关联环境ID
     */
    @Column(name = "environment_id")
    private Long environmentId;

    /**
     * 环境类型（关联字典 environment_type）
     */
    @Column(name = "environment_type")
    private String environmentType;

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
     * 配置信息（JSON格式）
     */
    @Column(length = 1000)
    private String configuration;

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
