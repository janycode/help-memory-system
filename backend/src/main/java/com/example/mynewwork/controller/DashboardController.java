package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.dto.DashboardStats;
import com.example.mynewwork.repository.EnvironmentRepository;
import com.example.mynewwork.repository.ProjectRepository;
import com.example.mynewwork.repository.TechnicalComponentRepository;
import com.example.mynewwork.repository.BusinessProcessRepository;
import com.example.mynewwork.repository.IterationRepository;
import com.example.mynewwork.service.ActivityLogService;
import com.example.mynewwork.model.entity.ActivityLog;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 仪表盘控制器
 *
 * 提供仪表盘相关数据接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "仪表盘相关接口")
public class DashboardController {

    private final EnvironmentRepository environmentRepository;
    private final TechnicalComponentRepository technicalComponentRepository;
    private final BusinessProcessRepository businessProcessRepository;
    private final ProjectRepository projectRepository;
    private final IterationRepository iterationRepository;
    private final ActivityLogService activityLogService;

    /**
     * 调试接口：检查数据库中的实际数据
     */
    @GetMapping("/debug/data-check")
    @Operation(summary = "数据检查", description = "检查各表中的数据情况")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugDataCheck() {
        log.info("执行数据检查");

        Map<String, Object> debugInfo = new HashMap<>();

        // 检查环境表
        long totalEnvs = environmentRepository.count();
        long activeEnvs = environmentRepository.countByActiveTrue();
        debugInfo.put("environments_total", totalEnvs);
        debugInfo.put("environments_active", activeEnvs);

        // 检查技术组件表
        long totalComponents = technicalComponentRepository.count();
        long activeComponents = technicalComponentRepository.countByActiveTrue();
        debugInfo.put("components_total", totalComponents);
        debugInfo.put("components_active", activeComponents);

        // 检查业务流程表
        long totalProcesses = businessProcessRepository.count();
        long activeProcesses = businessProcessRepository.countByActiveTrue();
        debugInfo.put("processes_total", totalProcesses);
        debugInfo.put("processes_active", activeProcesses);

        // 检查项目表
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByActiveTrue();
        debugInfo.put("projects_total", totalProjects);
        debugInfo.put("projects_active", activeProjects);

        return ResponseEntity.ok(ApiResponse.success(debugInfo, "数据检查完成"));
    }

    /**
     * 获取最近活动
     */
    @GetMapping("/recent-activities")
    @Operation(summary = "获取最近活动", description = "获取最近的用户活动日志")
    public ResponseEntity<ApiResponse<Object>> getRecentActivities(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("获取最近{}条活动", limit);

        try {
            List<ActivityLog> activities = activityLogService.findRecentActivities(limit);

            // 转换为前端需要的格式
            Object result = activities.stream().map(activity -> {
                Map<String, Object> activityMap = new HashMap<>();
                activityMap.put("id", activity.getId());
                activityMap.put("timestamp", activity.getCreatedAt());
                activityMap.put("type", getActivityType(activity.getOperation()));
                activityMap.put("description", activity.getUsername() + " " + activity.getDescription());
                return activityMap;
            }).toList();

            return ResponseEntity.ok(ApiResponse.success(result, "查询成功"));
        } catch (Exception e) {
            log.error("获取最近活动时发生错误", e);
            return ResponseEntity.ok(ApiResponse.error("获取最近活动失败"));
        }
    }

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计数据", description = "获取各模块的统计数据")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        log.info("获取仪表盘统计数据");

        try {
            DashboardStats stats = new DashboardStats();

            // 查询各模块的活跃数据数量
            long environmentCount = environmentRepository.countByActiveTrue();
            long componentCount = technicalComponentRepository.countByActiveTrue();
            long processCount = businessProcessRepository.countByActiveTrue();
            long projectCount = projectRepository.countByActiveTrue();
            long iterationCount = iterationRepository.countByActiveTrue();

            log.debug("统计数据 - 环境: {}, 组件: {}, 流程: {}, 项目: {}, 迭代: {}",
                     environmentCount, componentCount, processCount, projectCount, iterationCount);

            stats.setEnvironments(environmentCount);
            stats.setComponents(componentCount);
            stats.setProcesses(processCount);
            stats.setProjects(projectCount);
            stats.setIterations(iterationCount);

            return ResponseEntity.ok(ApiResponse.success(stats, "查询成功"));
        } catch (Exception e) {
            log.error("获取统计数据时发生错误", e);
            return ResponseEntity.ok(ApiResponse.error("获取统计数据失败"));
        }
    }

    /**
     * 根据操作类型获取活动类型
     */
    private String getActivityType(String operation) {
        return switch (operation.toUpperCase()) {
            case "CREATE" -> "success";
            case "UPDATE" -> "warning";
            case "DELETE" -> "danger";
            case "VIEW" -> "info";
            default -> "primary";
        };
    }
}
