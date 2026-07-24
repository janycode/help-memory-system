package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.User;
import com.example.mynewwork.security.JwtTokenProvider;
import com.example.mynewwork.security.UserPrincipal;
import com.example.mynewwork.service.ActivityLogService;
import com.example.mynewwork.service.UserMenuPermissionService;
import com.example.mynewwork.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final UserMenuPermissionService userMenuPermissionService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码进行登录")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest request) {
        log.info("用户登录: {}", loginRequest.getUsername());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        org.springframework.security.core.Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        userService.updateLastLoginTime(user.getId());
        
        activityLogService.log(user.getId(), user.getUsername(), "LOGIN", "用户认证", 
                "用户登录成功", null, request);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        data.put("allowedMenus", userMenuPermissionService.getAllowedMenus(user.getId()));

        return ResponseEntity.ok(ApiResponse.success(data, "登录成功"));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ResponseEntity<ApiResponse<User>> register(
            @RequestBody @Valid RegisterRequest registerRequest,
            HttpServletRequest request) {
        log.info("用户注册: {}", registerRequest.getUsername());

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFullName(registerRequest.getFullName());
        user.setDepartment(registerRequest.getDepartment());
        user.setPosition(registerRequest.getPosition());

        User createdUser = userService.createUser(user);
        
        activityLogService.log(createdUser.getId(), createdUser.getUsername(), "REGISTER", "用户认证", 
                "用户注册成功", null, request);

        return ResponseEntity.ok(ApiResponse.success(createdUser, "注册成功"));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的信息")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        log.info("修改密码: userId={}", userPrincipal.getUser().getId());

        userService.changePassword(userPrincipal.getUser().getId(), request.getOldPassword(), request.getNewPassword());
        
        activityLogService.log(userPrincipal.getUser().getId(), userPrincipal.getUser().getUsername(), 
                "UPDATE_PASSWORD", "用户认证", "修改密码", null, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        
        if (userPrincipal != null && userPrincipal.getUser() != null) {
            activityLogService.log(userPrincipal.getUser().getId(), userPrincipal.getUser().getUsername(), 
                    "LOGOUT", "用户认证", "用户登出", null, request);
        }
        
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
    }

    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String fullName;
        private String department;
        private String position;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
