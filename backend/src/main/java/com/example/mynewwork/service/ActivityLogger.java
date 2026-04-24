package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 活动日志记录器
 *
 * 提供便捷的方法来记录用户活动
 */
@Component
@RequiredArgsConstructor
public class ActivityLogger {

    private final ActivityLogService activityLogService;

    /**
     * 记录创建活动
     */
    public void logCreate(Long userId, String username, String module, String entityName) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation("CREATE");
        log.setModule(module);
        log.setDescription("创建了" + getModuleName(module) + ": " + entityName);
        activityLogService.createActivityLog(log);
    }

    /**
     * 记录更新活动
     */
    public void logUpdate(Long userId, String username, String module, String entityName) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation("UPDATE");
        log.setModule(module);
        log.setDescription("更新了" + getModuleName(module) + ": " + entityName);
        activityLogService.createActivityLog(log);
    }

    /**
     * 记录删除活动
     */
    public void logDelete(Long userId, String username, String module, String entityName) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation("DELETE");
        log.setModule(module);
        log.setDescription("删除了" + getModuleName(module) + ": " + entityName);
        activityLogService.createActivityLog(log);
    }

    /**
     * 记录查看活动
     */
    public void logView(Long userId, String username, String module, String entityName) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation("VIEW");
        log.setModule(module);
        log.setDescription("查看了" + getModuleName(module) + ": " + entityName);
        activityLogService.createActivityLog(log);
    }

    /**
     * 获取模块的中文名称
     */
    private String getModuleName(String module) {
        return switch (module.toUpperCase()) {
            case "ENVIRONMENT" -> "环境";
            case "COMPONENT" -> "技术组件";
            case "PROCESS" -> "业务流程";
            case "PROJECT" -> "项目";
            default -> module;
        };
    }
}