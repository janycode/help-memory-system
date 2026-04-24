package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 活动日志实体
 *
 * 记录用户操作和系统活动
 */
@Entity
@Table(name = "activity_logs")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class ActivityLog {

    /**
     * 日志ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 用户名
     */
    @Column(name = "username", length = 50)
    private String username;

    /**
     * 操作类型（CREATE/UPDATE/DELETE/VIEW）
     */
    @NotBlank
    @Column(nullable = false)
    private String operation;

    /**
     * 操作模块（ENVIRONMENT/COMPONENT/PROCESS/PROJECT）
     */
    @NotBlank
    @Column(nullable = false)
    private String module;

    /**
     * 操作描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 操作详情（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * 客户端IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 是否成功
     */
    @Column
    private Boolean success = true;
}