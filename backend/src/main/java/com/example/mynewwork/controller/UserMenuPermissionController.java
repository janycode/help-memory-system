package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.service.UserMenuPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-menu-permissions")
@RequiredArgsConstructor
public class UserMenuPermissionController {

    private final UserMenuPermissionService service;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<String>> getMenus(@PathVariable Long userId) {
        return ApiResponse.success(service.getAllowedMenus(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> saveMenus(@PathVariable Long userId, @RequestBody List<String> menus) {
        service.saveAllowedMenus(userId, menus);
        return ApiResponse.success(null, "保存成功");
    }

    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getAllUsersPermissions() {
        return ApiResponse.success(service.getAllUsersWithPermissions());
    }
}
