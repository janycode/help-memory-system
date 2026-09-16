package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.dto.GitHubConfigDTO;
import com.example.mynewwork.model.dto.GitHubIssueDTO;
import com.example.mynewwork.model.dto.GitHubIssueDetailDTO;
import com.example.mynewwork.model.dto.GitHubPromptResultDTO;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ActivityLogger;
import com.example.mynewwork.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GitHub 指派任务接口
 * 拉取指派给当前账号的 Issue 列表/详情并生成可复制的 AI 提示词
 *
 * @author jiangyuan
 */
@Slf4j
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub指派任务", description = "拉取指派给当前账号的 GitHub Issue 并生成 AI 提示词")
public class GitHubController {

    private final GitHubService gitHubService;
    private final ActivityLogger activityLogger;

    @GetMapping("/config")
    @Operation(summary = "读取 GitHub 配置")
    public ResponseEntity<ApiResponse<GitHubConfigDTO>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(gitHubService.getConfig()));
    }

    @PostMapping("/config")
    @Operation(summary = "保存 GitHub 配置")
    public ResponseEntity<ApiResponse<GitHubConfigDTO>> saveConfig(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody GitHubConfigDTO config) {
        GitHubConfigDTO saved = gitHubService.saveConfig(config);
        activityLogger.logUpdate(userId(userPrincipal), username(userPrincipal), "GITHUB", "GitHub 配置");
        return ResponseEntity.ok(ApiResponse.success(saved, "配置保存成功"));
    }

    @PostMapping("/connection-test")
    @Operation(summary = "测试连接", description = "调用 GitHub /user 验证 Token 并返回当前登录名")
    public ResponseEntity<ApiResponse<Map<String, String>>> testConnection() {
        Map<String, String> result = gitHubService.testConnection();
        return ResponseEntity.ok(ApiResponse.success(result, "连接成功，当前登录账号：" + result.get("login")));
    }

    @GetMapping("/issues")
    @Operation(summary = "指派给当前账号的 Issue 列表", description = "过滤 PR，open 排前 closed 排后，组内按更新时间倒序")
    public ResponseEntity<ApiResponse<List<GitHubIssueDTO>>> listIssues(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GitHubIssueDTO> issues = gitHubService.listIssues();
        activityLogger.logView(userId(userPrincipal), username(userPrincipal), "GITHUB", "指派任务列表");
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    @GetMapping("/issues/{number}")
    @Operation(summary = "Issue 详情", description = "正文 + 评论 + 图片（下载到本地）")
    public ResponseEntity<ApiResponse<GitHubIssueDetailDTO>> getDetail(@PathVariable int number) {
        return ResponseEntity.ok(ApiResponse.success(gitHubService.getDetail(number)));
    }

    @PostMapping("/issues/{number}/prompt")
    @Operation(summary = "生成 AI 提示词", description = "拉取详情并生成可直接复制给 AI Agent 的提示词")
    public ResponseEntity<ApiResponse<GitHubPromptResultDTO>> buildPrompt(
            @PathVariable int number,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitHubPromptResultDTO result = gitHubService.buildPrompt(number);
        activityLogger.logUpdate(userId(userPrincipal), username(userPrincipal), "GITHUB", "#" + number + " AI 提示词");
        return ResponseEntity.ok(ApiResponse.success(result, "提示词生成成功"));
    }

    @GetMapping("/issues/{number}/prompt/custom")
    @Operation(summary = "读取自定义提示词", description = "返回用户编辑并保存过的提示词；未编辑过返回 data=null")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCustomPrompt(@PathVariable int number) {
        String custom = gitHubService.getCustomPrompt(number);
        return ResponseEntity.ok(ApiResponse.success(Map.of("prompt", custom != null ? custom : "")));
    }

    @PutMapping("/issues/{number}/prompt/custom")
    @Operation(summary = "保存自定义提示词", description = "将用户编辑后的提示词持久化，下次打开优先展示")
    public ResponseEntity<ApiResponse<Void>> saveCustomPrompt(
            @PathVariable int number,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {
        String content = request.get("prompt");
        gitHubService.saveCustomPrompt(number, content);
        activityLogger.logUpdate(userId(userPrincipal), username(userPrincipal), "GITHUB", "#" + number + " 自定义提示词保存");
        return ResponseEntity.ok(ApiResponse.success(null, "提示词已保存"));
    }

    private Long userId(UserPrincipal userPrincipal) {
        return userPrincipal != null && userPrincipal.getUser() != null ? userPrincipal.getUser().getId() : null;
    }

    private String username(UserPrincipal userPrincipal) {
        return userPrincipal != null ? userPrincipal.getUsername() : null;
    }
}