package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.service.BusinessProcessService;
import com.example.mynewwork.service.EnvironmentService;
import com.example.mynewwork.service.ProjectService;
import com.example.mynewwork.service.TechnicalComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局搜索控制器
 *
 * 提供跨模块的统一搜索功能
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "全局搜索", description = "全局搜索相关接口")
public class SearchController {

    private final EnvironmentService environmentService;
    private final TechnicalComponentService technicalComponentService;
    private final BusinessProcessService businessProcessService;
    private final ProjectService projectService;

    /**
     * 全局搜索
     */
    @GetMapping
    @Operation(summary = "全局搜索", description = "跨模块搜索所有内容")
    public ResponseEntity<ApiResponse<SearchResult>> search(@RequestParam String q) {
        log.info("全局搜索: {}", q);

        SearchResult result = new SearchResult();
        result.setKeyword(q);
        result.setEnvironments(environmentService.searchByKeyword(q));
        result.setComponents(technicalComponentService.searchByKeyword(q));
        result.setProcesses(businessProcessService.searchByKeyword(q));
        result.setProjects(projectService.searchByKeyword(q));
        result.setTotal(result.getEnvironments().size()
                + result.getComponents().size()
                + result.getProcesses().size()
                + result.getProjects().size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索结果 DTO
     */
    @Data
    public static class SearchResult {
        private String keyword;
        private List<?> environments = new ArrayList<>();
        private List<?> components = new ArrayList<>();
        private List<?> processes = new ArrayList<>();
        private List<?> projects = new ArrayList<>();
        private Integer total;
    }
}
