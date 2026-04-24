package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.Environment;
import com.example.mynewwork.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调试控制器
 * 用于测试和调试
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final EnvironmentService environmentService;

    /**
     * 测试环境数据返回
     */
    @GetMapping("/environments")
    public ResponseEntity<ApiResponse<List<Environment>>> getEnvironmentsForDebug() {
        List<Environment> environments = environmentService.findAllActive();

        // 打印第一个环境的详细信息到日志
        if (!environments.isEmpty()) {
            Environment env = environments.get(0);
            log.info("环境数据详情: ID={}, Name={}, URL={}, Username={}, Password={}",
                    env.getId(), env.getName(), env.getUrl(), env.getUsername(), env.getPassword());
        }

        return ResponseEntity.ok(ApiResponse.success(environments));
    }
}