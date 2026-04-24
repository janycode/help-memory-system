package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.SystemConfig;
import com.example.mynewwork.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class SystemConfigController {

    private final SystemConfigService configService;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @GetMapping("/config")
    @Operation(summary = "获取所有系统配置")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAllConfigs() {
        List<SystemConfig> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/config/{key}")
    @Operation(summary = "根据key获取配置")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfigByKey(@PathVariable String key) {
        SystemConfig config = configService.getConfigByKey(key)
                .orElseThrow(() -> new RuntimeException("配置项不存在"));
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping("/title")
    @Operation(summary = "获取系统标题")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSystemTitle() {
        String title = configService.getConfigValue("system.name", "新人筑基丹");
        return ResponseEntity.ok(ApiResponse.success(Map.of("title", title)));
    }

    @PutMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新系统配置（管理员权限）")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfig(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String value = request.get("value");
        SystemConfig config = configService.updateConfig(id, value);
        log.info("更新系统配置: {} = {}", config.getConfigKey(), value);
        return ResponseEntity.ok(ApiResponse.success(config, "配置更新成功"));
    }

    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建系统配置（管理员权限）")
    public ResponseEntity<ApiResponse<SystemConfig>> createConfig(@RequestBody SystemConfig config) {
        SystemConfig saved = configService.setConfig(
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription()
        );
        log.info("创建系统配置: {} = {}", config.getConfigKey(), config.getConfigValue());
        return ResponseEntity.ok(ApiResponse.success(saved, "配置创建成功"));
    }

    @DeleteMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除系统配置（管理员权限）")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        log.info("删除系统配置: {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "配置删除成功"));
    }

    @GetMapping("/database/path")
    @Operation(summary = "获取数据库文件路径")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDatabasePath() {
        String filePath = "";
        if (databaseUrl.startsWith("jdbc:h2:file:")) {
            // 提取文件路径部分
            String relativePath = databaseUrl.substring("jdbc:h2:file:".length());
            // 移除可能的参数
            if (relativePath.contains(";")) {
                relativePath = relativePath.substring(0, relativePath.indexOf(";"));
            }
            // 转换为绝对路径
            try {
                File file = new File(relativePath);
                // 使用getCanonicalPath()获取规范化路径，消除路径中的点号和相对路径符号
                String canonicalPath = file.getCanonicalPath();
                // 确保路径中不包含多余的点
                if (canonicalPath.endsWith(".mv.db")) {
                    filePath = canonicalPath;
                } else {
                    filePath = canonicalPath + ".mv.db";
                }
            } catch (Exception e) {
                // 发生异常时使用绝对路径
                File file = new File(relativePath);
                String absolutePath = file.getAbsolutePath();
                if (absolutePath.endsWith(".mv.db")) {
                    filePath = absolutePath;
                } else {
                    filePath = absolutePath + ".mv.db";
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("path", filePath)));
    }
}
