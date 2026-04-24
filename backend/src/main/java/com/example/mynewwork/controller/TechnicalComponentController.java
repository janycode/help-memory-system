package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.dto.HealthCheckResult;
import com.example.mynewwork.model.entity.TechnicalComponent;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.TechnicalComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 技术组件管理控制器
 *
 * 处理技术组件的增删改查操作
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Tag(name = "技术组件管理", description = "技术组件相关接口")
public class TechnicalComponentController {

    private final TechnicalComponentService technicalComponentService;

    /**
     * 分页查询技术组件列表
     */
    @GetMapping
    @Operation(summary = "分页查询组件", description = "获取技术组件分页列表")
    public ResponseEntity<ApiResponse<Page<TechnicalComponent>>> getComponents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String environmentType) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TechnicalComponent> components = technicalComponentService.findAll(category, environmentType, pageable);
        return ResponseEntity.ok(ApiResponse.success(components));
    }

    /**
     * 查询所有活跃技术组件
     */
    @GetMapping("/active")
    @Operation(summary = "查询活跃组件", description = "获取所有活跃技术组件列表")
    public ResponseEntity<ApiResponse<List<TechnicalComponent>>> getActiveComponents() {
        List<TechnicalComponent> components = technicalComponentService.findAllActive();
        return ResponseEntity.ok(ApiResponse.success(components));
    }

    /**
     * 根据分类查询技术组件
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询组件", description = "根据分类获取技术组件列表")
    public ResponseEntity<ApiResponse<List<TechnicalComponent>>> getComponentsByCategory(
            @PathVariable String category) {
        List<TechnicalComponent> components = technicalComponentService.findByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(components));
    }

    /**
     * 根据环境ID查询技术组件
     */
    @GetMapping("/environment/{environmentId}")
    @Operation(summary = "按环境查询组件", description = "根据环境ID获取技术组件列表")
    public ResponseEntity<ApiResponse<List<TechnicalComponent>>> getComponentsByEnvironment(
            @PathVariable Long environmentId) {
        List<TechnicalComponent> components = technicalComponentService.findByEnvironmentId(environmentId);
        return ResponseEntity.ok(ApiResponse.success(components));
    }

    /**
     * 根据ID查询技术组件
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询组件详情", description = "根据ID获取技术组件详情")
    public ResponseEntity<ApiResponse<TechnicalComponent>> getComponent(@PathVariable Long id) {
        TechnicalComponent component = technicalComponentService.findById(id)
                .orElseThrow(() -> new RuntimeException("技术组件不存在"));
        return ResponseEntity.ok(ApiResponse.success(component));
    }

    /**
     * 搜索技术组件
     */
    @GetMapping("/search")
    @Operation(summary = "搜索组件", description = "根据关键字搜索技术组件")
    public ResponseEntity<ApiResponse<List<TechnicalComponent>>> searchComponents(
            @RequestParam String keyword) {
        List<TechnicalComponent> components = technicalComponentService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(components));
    }

    /**
     * 创建技术组件
     */
    @PostMapping
    @Operation(summary = "创建组件", description = "创建新的技术组件")
    public ResponseEntity<ApiResponse<TechnicalComponent>> createComponent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid TechnicalComponent component) {

        Long userId = userPrincipal.getUser().getId();
        TechnicalComponent created = technicalComponentService.createComponent(component, userId);
        return ResponseEntity.ok(ApiResponse.success(created, "组件创建成功"));
    }

    /**
     * 更新技术组件
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新组件", description = "更新技术组件信息")
    public ResponseEntity<ApiResponse<TechnicalComponent>> updateComponent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid TechnicalComponent component) {

        Long userId = userPrincipal.getUser().getId();
        TechnicalComponent updated = technicalComponentService.updateComponent(id, component, userId);
        return ResponseEntity.ok(ApiResponse.success(updated, "组件更新成功"));
    }

    /**
     * 健康检查
     */
    @GetMapping("/{id}/health")
    @Operation(summary = "连通性检测", description = "检测技术组件地址是否可达")
    public ResponseEntity<ApiResponse<HealthCheckResult>> checkHealth(@PathVariable Long id) {
        HealthCheckResult result = technicalComponentService.checkHealth(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除技术组件（软删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除组件", description = "软删除技术组件")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(@PathVariable Long id) {
        technicalComponentService.deleteComponent(id);
        return ResponseEntity.ok(ApiResponse.success(null, "组件删除成功"));
    }

    /**
     * 永久删除技术组件
     */
    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除组件", description = "永久删除技术组件")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteComponent(@PathVariable Long id) {
        technicalComponentService.permanentlyDeleteComponent(id);
        return ResponseEntity.ok(ApiResponse.success(null, "组件已永久删除"));
    }
}
