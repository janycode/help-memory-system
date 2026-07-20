package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.User;
import com.example.mynewwork.model.entity.UserMenuPermission;
import com.example.mynewwork.repository.UserMenuPermissionRepository;
import com.example.mynewwork.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMenuPermissionService {

    private final UserMenuPermissionRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> ALL_MENUS = List.of(
            "home", "iterations", "search", "database",
            "environments", "components", "processes", "repositories",
            "snippets", "batch-so", "mq-send", "dict", "users", "menu-permissions", "system"
    );

    private static final List<String> DEFAULT_MENUS = List.of("home");

    public List<String> getAllowedMenus(Long userId) {
        return repository.findByUserId(userId)
                .map(p -> parseMenus(p.getAllowedMenus()))
                .orElse(new ArrayList<>(DEFAULT_MENUS));
    }

    @Transactional
    public void saveAllowedMenus(Long userId, List<String> menus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserMenuPermission permission = repository.findByUserId(userId)
                .orElse(new UserMenuPermission());

        permission.setUser(user);
        try {
            permission.setAllowedMenus(objectMapper.writeValueAsString(menus));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("菜单权限序列化失败", e);
        }

        if (permission.getCreatedAt() == null) {
            permission.setCreatedAt(LocalDateTime.now());
        }
        permission.setUpdatedAt(LocalDateTime.now());

        repository.save(permission);
        log.info("保存用户菜单权限: userId={}, menus={}", userId, menus);
    }

    public List<UserMenuPermission> getAllPermissions() {
        return repository.findAll();
    }

    public List<Map<String, Object>> getAllUsersWithPermissions() {
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : allUsers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("fullName", user.getFullName());
            item.put("allowedMenus", getAllowedMenus(user.getId()));
            result.add(item);
        }
        return result;
    }

    private List<String> parseMenus(String menusJson) {
        try {
            return objectMapper.readValue(menusJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析菜单权限失败: {}", menusJson, e);
            return new ArrayList<>(ALL_MENUS);
        }
    }
}
