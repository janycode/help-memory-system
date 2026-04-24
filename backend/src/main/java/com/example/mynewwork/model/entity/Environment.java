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
 * 环境配置实体
 *
 * 管理开发、测试、预发布、生产等环境配置信息
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Entity
@Table(name = "environments")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class Environment {

    /**
     * 环境ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 环境名称
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * 环境描述
     */
    @Column
    private String description;

    /**
     * 环境类型（DEV/TEST/STAGING/PROD）
     */
    @Column(nullable = false)
    private String type;

    /**
     * 环境访问地址
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
     * 数据库连接地址
     */
    @Column
    private String databaseUrl;

    /**
     * 数据库用户名
     */
    @Column
    private String databaseUsername;

    /**
     * 数据库密码（加密存储）
     */
    @Column
    private String databasePassword;

    /**
     * Redis连接地址
     */
    @Column
    private String redisUrl;

    /**
     * Redis密码（加密存储）
     */
    @Column
    private String redisPassword;

    /**
     * 消息队列连接地址
     */
    @Column
    private String mqUrl;

    /**
     * 消息队列用户名
     */
    @Column
    private String mqUsername;

    /**
     * 消息队列密码（加密存储）
     */
    @Column
    private String mqPassword;

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
