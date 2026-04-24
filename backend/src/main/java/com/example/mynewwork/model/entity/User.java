package com.example.mynewwork.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class User {

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 邮箱
     */
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * 密码（加密存储）
     */
    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;

    /**
     * 全名
     */
    @Column
    private String fullName;

    /**
     * 部门
     */
    @Column
    private String department;

    /**
     * 职位
     */
    @Column
    private String position;

    /**
     * 角色列表
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    /**
     * 是否启用
     */
    @Column
    private Boolean enabled = true;

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
     * 最后登录时间
     */
    @Column
    private LocalDateTime lastLoginAt;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;
}