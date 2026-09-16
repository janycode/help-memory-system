package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.JenkinsJobConfig;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ActivityLogger;
import com.example.mynewwork.service.JenkinsMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Jenkins 监控配置管理接口
 *
 * @author jiangyuan
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/jenkins-monitor")
@RequiredArgsConstructor
@Tag(name = "Jenkins监控", description = "Jenkins 构建成功通知配置")
public class JenkinsMonitorController {

    private final JenkinsMonitorService jenkinsMonitorService;
    private final ActivityLogger activityLogger;

    @GetMapping
    @Operation(summary = "查询监控配置列表")
    public ResponseEntity<ApiResponse<List<JenkinsJobConfig>>> list() {
        return ResponseEntity.ok(ApiResponse.success(jenkinsMonitorService.listConfigs()));
    }

    @PostMapping
    @Operation(summary = "新增监控配置")
    public ResponseEntity<ApiResponse<JenkinsJobConfig>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody JenkinsJobConfig config) {
        try {
            JenkinsJobConfig saved = jenkinsMonitorService.createConfig(config);
            activityLogger.logCreate(userId(userPrincipal), username(userPrincipal), "JENKINS_MONITOR", config.getJobName());
            return ResponseEntity.ok(ApiResponse.success(saved, "配置创建成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新监控配置")
    public ResponseEntity<ApiResponse<JenkinsJobConfig>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody JenkinsJobConfig config) {
        try {
            JenkinsJobConfig updated = jenkinsMonitorService.updateConfig(id, config);
            activityLogger.logUpdate(userId(userPrincipal), username(userPrincipal), "JENKINS_MONITOR", config.getJobName());
            return ResponseEntity.ok(ApiResponse.success(updated, "配置更新成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除监控配置")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        jenkinsMonitorService.deleteConfig(id);
        activityLogger.logDelete(userId(userPrincipal), username(userPrincipal), "JENKINS_MONITOR", String.valueOf(id));
        return ResponseEntity.ok(ApiResponse.success(null, "配置删除成功"));
    }

    @PostMapping("/test")
    @Operation(summary = "测试 Jenkins 连接", description = "校验账号密码并返回可见 job 列表，密码为空时按 id 取已保存配置，传 jobName 时校验 job 是否存在")
    public ResponseEntity<ApiResponse<Map<String, Object>>> test(@RequestBody Map<String, Object> request) {
        try {
            String jenkinsUrl = (String) request.get("jenkinsUrl");
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String jobName = (String) request.get("jobName");
            Long configId = request.get("id") != null
                    ? Long.valueOf(String.valueOf(request.get("id"))) : null;
            Map<String, Object> result = jenkinsMonitorService.testConnection(jenkinsUrl, username, password, configId, jobName);
            return ResponseEntity.ok(ApiResponse.success(result, "连接成功"));
        } catch (Exception e) {
            log.warn("Jenkins 连接测试失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("连接失败: " + e.getMessage()));
        }
    }

    private Long userId(UserPrincipal userPrincipal) {
        return userPrincipal != null && userPrincipal.getUser() != null ? userPrincipal.getUser().getId() : null;
    }

    private String username(UserPrincipal userPrincipal) {
        return userPrincipal != null ? userPrincipal.getUsername() : null;
    }
}