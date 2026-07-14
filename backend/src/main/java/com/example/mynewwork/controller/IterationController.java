package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.Iteration;
import com.example.mynewwork.model.entity.IterationImportConfig;
import com.example.mynewwork.model.entity.IterationSyncHistory;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.IterationImportService;
import com.example.mynewwork.service.IterationService;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/iterations")
@RequiredArgsConstructor
@Tag(name = "迭代管理", description = "迭代管理相关接口")
public class IterationController {

    private final IterationService iterationService;
    private final IterationImportService iterationImportService;

    @GetMapping
    @Operation(summary = "分页查询迭代列表", description = "获取迭代分页列表")
    public ResponseEntity<ApiResponse<Page<Iteration>>> getIterations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Iteration> iterations = iterationService.findAll(status, priority, pageable);
        return ResponseEntity.ok(ApiResponse.success(iterations));
    }

    @GetMapping("/active")
    @Operation(summary = "查询活跃迭代", description = "获取所有活跃迭代列表")
    public ResponseEntity<ApiResponse<List<Iteration>>> getActiveIterations() {
        List<Iteration> iterations = iterationService.findAllActive();
        return ResponseEntity.ok(ApiResponse.success(iterations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询迭代详情", description = "根据ID获取迭代详情")
    public ResponseEntity<ApiResponse<Iteration>> getIteration(@PathVariable Long id) {
        Iteration iteration = iterationService.findById(id)
                .orElseThrow(() -> new RuntimeException("迭代不存在"));
        return ResponseEntity.ok(ApiResponse.success(iteration));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索迭代", description = "根据关键字搜索迭代")
    public ResponseEntity<ApiResponse<List<Iteration>>> searchIterations(
            @RequestParam String keyword) {
        List<Iteration> iterations = iterationService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(iterations));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态查询迭代", description = "根据状态获取迭代列表")
    public ResponseEntity<ApiResponse<List<Iteration>>> getIterationsByStatus(
            @PathVariable String status) {
        List<Iteration> iterations = iterationService.findByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(iterations));
    }

    @GetMapping("/priority/{priority}")
    @Operation(summary = "按优先级查询迭代", description = "根据优先级获取迭代列表")
    public ResponseEntity<ApiResponse<List<Iteration>>> getIterationsByPriority(
            @PathVariable String priority) {
        List<Iteration> iterations = iterationService.findByPriority(priority);
        return ResponseEntity.ok(ApiResponse.success(iterations));
    }

    @PostMapping
    @Operation(summary = "创建迭代", description = "创建新的迭代")
    public ResponseEntity<ApiResponse<Iteration>> createIteration(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid Iteration iteration) {

        Long userId = userPrincipal != null ? userPrincipal.getUser().getId() : null;
        Iteration created = iterationService.createIteration(iteration, userId);
        return ResponseEntity.ok(ApiResponse.success(created, "迭代创建成功"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新迭代", description = "更新迭代信息")
    public ResponseEntity<ApiResponse<Iteration>> updateIteration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Iteration iteration) {

        Long userId = userPrincipal != null ? userPrincipal.getUser().getId() : null;
        Iteration updated = iterationService.updateIteration(id, iteration, userId);
        return ResponseEntity.ok(ApiResponse.success(updated, "迭代更新成功"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除迭代", description = "软删除迭代")
    public ResponseEntity<ApiResponse<Void>> deleteIteration(@PathVariable Long id) {
        iterationService.deleteIteration(id);
        return ResponseEntity.ok(ApiResponse.success(null, "迭代删除成功"));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除迭代", description = "永久删除迭代")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteIteration(@PathVariable Long id) {
        iterationService.permanentlyDeleteIteration(id);
        return ResponseEntity.ok(ApiResponse.success(null, "迭代已永久删除"));
    }

    @GetMapping("/import/config")
    @Operation(summary = "获取导入配置", description = "获取最近一次导入的目录配置")
    public ResponseEntity<ApiResponse<IterationImportConfig>> getImportConfig() {
        IterationImportConfig config = iterationImportService.getConfig().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping("/import")
    @Operation(summary = "从目录导入需求", description = "从指定目录导入需求到系统")
    public ResponseEntity<ApiResponse<List<Iteration>>> importFromDirectory(
            @RequestBody Map<String, String> request) {
        try {
            String dirPath = request.get("dirPath");
            if (dirPath == null || dirPath.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("目录路径不能为空"));
            }
            List<Iteration> imported = iterationImportService.importFromDirectory(dirPath);
            return ResponseEntity.ok(ApiResponse.success(imported, "导入成功，共导入 " + imported.size() + " 条需求"));
        } catch (IOException e) {
            log.error("导入需求IO异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("导入失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("导入需求异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("导入失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "同步需求到本地目录", description = "将单个需求同步到本地目录")
    public ResponseEntity<ApiResponse<Void>> syncIterationToLocal(@PathVariable Long id) {
        try {
            Iteration iteration = iterationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("迭代不存在"));
            iterationImportService.syncIterationToLocal(iteration);
            return ResponseEntity.ok(ApiResponse.success(null, "同步成功"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sync-all")
    @Operation(summary = "同步所有需求到本地目录", description = "将所有需求同步到本地目录")
    public ResponseEntity<ApiResponse<Void>> syncAllToLocal() {
        try {
            iterationImportService.syncAllToLocal();
            return ResponseEntity.ok(ApiResponse.success(null, "同步成功"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    @PostMapping("/auto-sync")
    @Operation(summary = "自动同步", description = "自动检测并同步页面和本地目录的变更")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoSync() {
        try {
            Map<String, Object> result = iterationImportService.autoSync();
            return ResponseEntity.ok(ApiResponse.success(result, "同步完成"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh-file-lists")
    @Operation(summary = "刷新文件列表", description = "刷新所有需求的文件列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshFileLists() {
        try {
            Map<String, Object> result = iterationImportService.refreshAllFileLists();
            return ResponseEntity.ok(ApiResponse.success(result, "刷新完成"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("刷新失败: " + e.getMessage()));
        }
    }

    @PostMapping("/check-new-folders")
    @Operation(summary = "检测新文件夹", description = "检测导入目录是否有新文件夹并导入")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkNewFolders() {
        try {
            Map<String, Object> result = iterationImportService.checkAndImportNewFolders();
            return ResponseEntity.ok(ApiResponse.success(result, "检测完成"));
        } catch (IOException e) {
            log.error("检测新文件夹IO异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("检测失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("检测新文件夹异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("检测失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/sync-history")
    @Operation(summary = "获取同步历史", description = "获取需求的同步历史记录")
    public ResponseEntity<ApiResponse<List<IterationSyncHistory>>> getSyncHistory(@PathVariable Long id) {
        List<IterationSyncHistory> history = iterationImportService.getSyncHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}