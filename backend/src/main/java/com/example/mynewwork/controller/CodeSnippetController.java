package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.CodeSnippet;
import com.example.mynewwork.service.CodeSnippetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
@Tag(name = "代码片段", description = "代码片段管理接口")
public class CodeSnippetController {

    private final CodeSnippetService codeSnippetService;

    @GetMapping
    @Operation(summary = "分页查询代码片段")
    public ResponseEntity<ApiResponse<Page<CodeSnippet>>> getSnippets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String language) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<CodeSnippet> snippets = codeSnippetService.findAll(language, pageable);
        return ResponseEntity.ok(ApiResponse.success(snippets));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有代码片段列表")
    public ResponseEntity<ApiResponse<List<CodeSnippet>>> getAllSnippets() {
        List<CodeSnippet> snippets = codeSnippetService.findAllList();
        return ResponseEntity.ok(ApiResponse.success(snippets));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取代码片段")
    public ResponseEntity<ApiResponse<CodeSnippet>> getSnippetById(@PathVariable Long id) {
        CodeSnippet snippet = codeSnippetService.findById(id)
                .orElseThrow(() -> new RuntimeException("代码片段不存在"));
        return ResponseEntity.ok(ApiResponse.success(snippet));
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "根据语言获取代码片段")
    public ResponseEntity<ApiResponse<List<CodeSnippet>>> getSnippetsByLanguage(@PathVariable String language) {
        List<CodeSnippet> snippets = codeSnippetService.findByLanguage(language);
        return ResponseEntity.ok(ApiResponse.success(snippets));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索代码片段")
    public ResponseEntity<ApiResponse<List<CodeSnippet>>> searchSnippets(@RequestParam String keyword) {
        List<CodeSnippet> snippets = codeSnippetService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(snippets));
    }

    @PostMapping
    @Operation(summary = "创建代码片段")
    public ResponseEntity<ApiResponse<CodeSnippet>> createSnippet(@RequestBody CodeSnippet codeSnippet) {
        CodeSnippet saved = codeSnippetService.save(codeSnippet);
        return ResponseEntity.ok(ApiResponse.success(saved, "创建成功"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新代码片段")
    public ResponseEntity<ApiResponse<CodeSnippet>> updateSnippet(
            @PathVariable Long id,
            @RequestBody CodeSnippet codeSnippet) {
        CodeSnippet updated = codeSnippetService.update(id, codeSnippet);
        return ResponseEntity.ok(ApiResponse.success(updated, "更新成功"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除代码片段")
    public ResponseEntity<ApiResponse<Void>> deleteSnippet(@PathVariable Long id) {
        codeSnippetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }
}
