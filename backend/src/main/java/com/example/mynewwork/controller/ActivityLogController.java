package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.ActivityLog;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "活动日志", description = "活动日志查询接口")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "分页查询活动日志")
    public ResponseEntity<ApiResponse<Page<ActivityLog>>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ActivityLog> logs = activityLogService.getActivityLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近活动日志")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        List<ActivityLog> logs = activityLogService.getRecentActivities(limit);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/my")
    @Operation(summary = "获取当前用户的活动日志")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getMyActivities(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ActivityLog> logs = activityLogService.getActivitiesByUser(userPrincipal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "根据模块查询活动日志")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getActivitiesByModule(
            @PathVariable String module) {
        List<ActivityLog> logs = activityLogService.getActivitiesByModule(module);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
