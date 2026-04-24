package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.Project;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ProjectService;
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
 * 代码仓库控制器
 *
 * 处理代码仓库信息的增删改查操作
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Tag(name = "代码仓库", description = "代码仓库信息相关接口")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 分页查询代码仓库列表
     */
    @GetMapping
    @Operation(summary = "分页查询代码仓库", description = "获取代码仓库分页列表")
    public ResponseEntity<ApiResponse<Page<Project>>> getRepositories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Project> repositories = projectService.findAll(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(repositories));
    }

    /**
     * 查询所有活跃代码仓库
     */
    @GetMapping("/active")
    @Operation(summary = "查询活跃代码仓库", description = "获取所有活跃代码仓库列表")
    public ResponseEntity<ApiResponse<List<Project>>> getActiveRepositories() {
        List<Project> repositories = projectService.findAllActive();
        return ResponseEntity.ok(ApiResponse.success(repositories));
    }

    /**
     * 根据代码仓库全称查询代码仓库
     */
    @GetMapping("/fullname/{fullname}")
    @Operation(summary = "按代码仓库全称查询代码仓库", description = "根据代码仓库全称获取代码仓库列表")
    public ResponseEntity<ApiResponse<List<Project>>> getRepositoriesByFullName(@PathVariable String fullname) {
        List<Project> repositories = projectService.findByProjectFullName(fullname);
        return ResponseEntity.ok(ApiResponse.success(repositories));
    }

    /**
     * 根据ID查询代码仓库
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询代码仓库详情", description = "根据ID获取代码仓库详情")
    public ResponseEntity<ApiResponse<Project>> getRepository(@PathVariable Long id) {
        Project repository = projectService.findById(id)
                .orElseThrow(() -> new RuntimeException("代码仓库不存在"));
        return ResponseEntity.ok(ApiResponse.success(repository));
    }

    /**
     * 搜索代码仓库
     */
    @GetMapping("/search")
    @Operation(summary = "搜索代码仓库", description = "根据关键字搜索代码仓库")
    public ResponseEntity<ApiResponse<List<Project>>> searchRepositories(
            @RequestParam String keyword) {
        List<Project> repositories = projectService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(repositories));
    }

    /**
     * 创建代码仓库
     */
    @PostMapping
    @Operation(summary = "创建代码仓库", description = "创建新的代码仓库")
    public ResponseEntity<ApiResponse<Project>> createRepository(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid Project project) {

        Long userId = userPrincipal.getUser().getId();
        Project created = projectService.createProject(project, userId);
        return ResponseEntity.ok(ApiResponse.success(created, "代码仓库创建成功"));
    }

    /**
     * 更新代码仓库
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新代码仓库", description = "更新代码仓库信息")
    public ResponseEntity<ApiResponse<Project>> updateRepository(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid Project project) {

        Long userId = userPrincipal.getUser().getId();
        Project updated = projectService.updateProject(id, project, userId);
        return ResponseEntity.ok(ApiResponse.success(updated, "代码仓库更新成功"));
    }

    /**
     * 删除代码仓库（软删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除代码仓库", description = "软删除代码仓库")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(null, "代码仓库删除成功"));
    }

    /**
     * 永久删除代码仓库
     */
    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除代码仓库", description = "永久删除代码仓库")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteRepository(@PathVariable Long id) {
        projectService.permanentlyDeleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(null, "代码仓库已永久删除"));
    }
}
