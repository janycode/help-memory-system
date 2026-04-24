package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.Environment;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ActivityLogService;
import com.example.mynewwork.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/environments")
@RequiredArgsConstructor
@Tag(name = "环境管理", description = "环境配置相关接口")
public class EnvironmentController {

    private final EnvironmentService environmentService;
    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "分页查询环境", description = "获取环境分页列表")
    public ResponseEntity<ApiResponse<Page<Environment>>> getEnvironments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String type) {

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Environment> environments = environmentService.findAll(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(environments));
    }

    @GetMapping("/active")
    @Operation(summary = "查询活跃环境", description = "获取所有活跃环境列表")
    public ResponseEntity<ApiResponse<List<Environment>>> getActiveEnvironments() {
        List<Environment> environments = environmentService.findAllActive();
        return ResponseEntity.ok(ApiResponse.success(environments));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "按类型查询环境", description = "根据环境类型获取环境列表")
    public ResponseEntity<ApiResponse<List<Environment>>> getEnvironmentsByType(@PathVariable String type) {
        List<Environment> environments = environmentService.findByType(type);
        return ResponseEntity.ok(ApiResponse.success(environments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询环境详情", description = "根据ID获取环境详情")
    public ResponseEntity<ApiResponse<Environment>> getEnvironment(@PathVariable Long id) {
        Environment environment = environmentService.findById(id)
                .orElseThrow(() -> new RuntimeException("环境不存在"));
        return ResponseEntity.ok(ApiResponse.success(environment));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索环境", description = "根据关键字搜索环境")
    public ResponseEntity<ApiResponse<List<Environment>>> searchEnvironments(
            @RequestParam String keyword) {
        List<Environment> environments = environmentService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(environments));
    }

    @PostMapping
    @Operation(summary = "创建环境", description = "创建新的环境配置")
    public ResponseEntity<ApiResponse<Environment>> createEnvironment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid Environment environment,
            HttpServletRequest request) {

        Long userId = userPrincipal.getUser().getId();
        String username = userPrincipal.getUser().getUsername();
        Environment created = environmentService.createEnvironment(environment, userId);
        
        activityLogService.log(userId, username, "CREATE", "环境管理", 
                "创建环境: " + created.getName(), created, request);
        
        return ResponseEntity.ok(ApiResponse.success(created, "环境创建成功"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新环境", description = "更新环境配置信息")
    public ResponseEntity<ApiResponse<Environment>> updateEnvironment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid Environment environment,
            HttpServletRequest request) {

        Long userId = userPrincipal.getUser().getId();
        String username = userPrincipal.getUser().getUsername();
        Environment updated = environmentService.updateEnvironment(id, environment, userId);
        
        activityLogService.log(userId, username, "UPDATE", "环境管理", 
                "更新环境: " + updated.getName(), updated, request);
        
        return ResponseEntity.ok(ApiResponse.success(updated, "环境更新成功"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除环境", description = "软删除环境配置")
    public ResponseEntity<ApiResponse<Void>> deleteEnvironment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        
        Environment environment = environmentService.findById(id)
                .orElseThrow(() -> new RuntimeException("环境不存在"));
        
        Long userId = userPrincipal.getUser().getId();
        String username = userPrincipal.getUser().getUsername();
        environmentService.deleteEnvironment(id);
        
        activityLogService.log(userId, username, "DELETE", "环境管理", 
                "删除环境: " + environment.getName(), null, request);
        
        return ResponseEntity.ok(ApiResponse.success(null, "环境删除成功"));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除环境", description = "永久删除环境配置")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteEnvironment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        
        Environment environment = environmentService.findById(id)
                .orElseThrow(() -> new RuntimeException("环境不存在"));
        
        Long userId = userPrincipal.getUser().getId();
        String username = userPrincipal.getUser().getUsername();
        environmentService.permanentlyDeleteEnvironment(id);
        
        activityLogService.log(userId, username, "DELETE", "环境管理", 
                "永久删除环境: " + environment.getName(), null, request);
        
        return ResponseEntity.ok(ApiResponse.success(null, "环境已永久删除"));
    }
}
