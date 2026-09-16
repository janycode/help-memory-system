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
 * Jenkins 监控配置实体
 *
 * 记录 Jenkins 连接信息与需要监控的 job，构建成功时推送企业微信通知
 *
 * @author jiangyuan
 * @since 1.0.0
 */
@Entity
@Table(name = "jenkins_job_config")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class JenkinsJobConfig {

    /**
     * 配置 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Jenkins job 名称
     */
    @NotBlank
    @Column(nullable = false)
    private String jobName;

    /**
     * Jenkins 地址
     */
    @Column(columnDefinition = "TEXT")
    private String jenkinsUrl;

    /**
     * Jenkins 账号
     */
    @Column
    private String username;

    /**
     * Jenkins 密码（明文存储，编辑弹窗可回显）
     */
    @Column
    private String password;

    /**
     * 部署环境标识（如 DEV/TEST/PROD，消息通知中展示）
     */
    @Column
    private String environment;

    /**
     * 项目本地 git 目录（绝对路径），用于取最近提交日志，留空则不展示
     */
    @Column(columnDefinition = "TEXT")
    private String localDir;

    /**
     * 企业微信机器人 webhook 地址
     */
    @Column(columnDefinition = "TEXT")
    private String webhookUrl;

    /**
     * 需 @ 的手机号（逗号分隔，可空）
     */
    @Column
    private String atMobiles;

    /**
     * 是否启用监控
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 已通知的最大构建号
     */
    @Column
    private Long lastNotifiedBuild;

    /**
     * 备注
     */
    @Column
    private String remark;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;
}