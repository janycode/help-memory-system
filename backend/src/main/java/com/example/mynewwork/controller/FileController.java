package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件操作", description = "文件读写相关接口")
public class FileController {

    @GetMapping("/read")
    @Operation(summary = "读取文件内容", description = "读取指定路径的文件内容")
    public ResponseEntity<ApiResponse<Map<String, String>>> readFile(@RequestParam String path) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("content", "", "path", path)));
            }
            String content = Files.readString(filePath);
            return ResponseEntity.ok(ApiResponse.success(Map.of("content", content, "path", path)));
        } catch (IOException e) {
            log.error("读取文件失败: {}", path, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("读取文件失败: " + e.getMessage()));
        }
    }

    @PostMapping("/write")
    @Operation(summary = "写入文件内容", description = "写入内容到指定路径的文件")
    public ResponseEntity<ApiResponse<Void>> writeFile(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String content = request.get("content");
        
        if (path == null || path.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("文件路径不能为空"));
        }
        
        try {
            Path filePath = Paths.get(path);
            // 确保目录存在
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content != null ? content : "");
            return ResponseEntity.ok(ApiResponse.success(null, "文件保存成功"));
        } catch (IOException e) {
            log.error("写入文件失败: {}", path, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("写入文件失败: " + e.getMessage()));
        }
    }

    @GetMapping("/check")
    @Operation(summary = "检查文件更新", description = "检查文件是否有更新")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFileUpdate(
            @RequestParam String path,
            @RequestParam long lastModified) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("exists", false, "updated", false)));
            }
            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
            boolean updated = currentModified > lastModified;
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "exists", true,
                    "updated", updated,
                    "lastModified", currentModified
            )));
        } catch (IOException e) {
            log.error("检查文件更新失败: {}", path, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("检查文件更新失败: " + e.getMessage()));
        }
    }
}
