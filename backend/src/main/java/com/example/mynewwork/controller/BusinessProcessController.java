package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.BusinessProcess;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.BusinessProcessService;
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
 * 业务流程管理控制器
 *
 * 处理业务流程的增删改查操作
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
@Tag(name = "业务流程管理", description = "业务流程相关接口")
public class BusinessProcessController {

    private final BusinessProcessService businessProcessService;

    /**
     * 分页查询业务流程列表
     */
    @GetMapping
    @Operation(summary = "分页查询流程", description = "获取业务流程分页列表")
    public ResponseEntity<ApiResponse<Page<BusinessProcess>>> getProcesses(
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

        Page<BusinessProcess> processes = businessProcessService.findAll(category, environmentType, pageable);
        return ResponseEntity.ok(ApiResponse.success(processes));
    }

    /**
     * 查询所有活跃业务流程
     */
    @GetMapping("/active")
    @Operation(summary = "查询活跃流程", description = "获取所有活跃业务流程列表")
    public ResponseEntity<ApiResponse<List<BusinessProcess>>> getActiveProcesses() {
        List<BusinessProcess> processes = businessProcessService.findAllActive();
        return ResponseEntity.ok(ApiResponse.success(processes));
    }

    /**
     * 查询按优先级排序的业务流程
     */
    @GetMapping("/priority")
    @Operation(summary = "按优先级查询流程", description = "获取按优先级排序的业务流程列表")
    public ResponseEntity<ApiResponse<List<BusinessProcess>>> getProcessesByPriority() {
        List<BusinessProcess> processes = businessProcessService.findAllOrderByPriority();
        return ResponseEntity.ok(ApiResponse.success(processes));
    }

    /**
     * 根据分类查询业务流程
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询流程", description = "根据分类获取业务流程列表")
    public ResponseEntity<ApiResponse<List<BusinessProcess>>> getProcessesByCategory(
            @PathVariable String category) {
        List<BusinessProcess> processes = businessProcessService.findByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(processes));
    }

    /**
     * 根据ID查询业务流程
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询流程详情", description = "根据ID获取业务流程详情")
    public ResponseEntity<ApiResponse<BusinessProcess>> getProcess(@PathVariable Long id) {
        BusinessProcess process = businessProcessService.findById(id)
                .orElseThrow(() -> new RuntimeException("业务流程不存在"));
        return ResponseEntity.ok(ApiResponse.success(process));
    }

    /**
     * 搜索业务流程
     */
    @GetMapping("/search")
    @Operation(summary = "搜索流程", description = "根据关键字搜索业务流程")
    public ResponseEntity<ApiResponse<List<BusinessProcess>>> searchProcesses(
            @RequestParam String keyword) {
        List<BusinessProcess> processes = businessProcessService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(processes));
    }

    /**
     * 创建业务流程
     */
    @PostMapping
    @Operation(summary = "创建流程", description = "创建新的业务流程")
    public ResponseEntity<ApiResponse<BusinessProcess>> createProcess(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid BusinessProcess process) {

        Long userId = userPrincipal.getUser().getId();
        BusinessProcess created = businessProcessService.createProcess(process, userId);
        return ResponseEntity.ok(ApiResponse.success(created, "流程创建成功"));
    }

    /**
     * 更新业务流程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新流程", description = "更新业务流程信息")
    public ResponseEntity<ApiResponse<BusinessProcess>> updateProcess(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid BusinessProcess process) {

        Long userId = userPrincipal.getUser().getId();
        BusinessProcess updated = businessProcessService.updateProcess(id, process, userId);
        return ResponseEntity.ok(ApiResponse.success(updated, "流程更新成功"));
    }

    /**
     * 删除业务流程（软删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除流程", description = "软删除业务流程")
    public ResponseEntity<ApiResponse<Void>> deleteProcess(@PathVariable Long id) {
        businessProcessService.deleteProcess(id);
        return ResponseEntity.ok(ApiResponse.success(null, "流程删除成功"));
    }

    /**
     * 永久删除业务流程
     */
    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除流程", description = "永久删除业务流程")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteProcess(@PathVariable Long id) {
        businessProcessService.permanentlyDeleteProcess(id);
        return ResponseEntity.ok(ApiResponse.success(null, "流程已永久删除"));
    }
}
