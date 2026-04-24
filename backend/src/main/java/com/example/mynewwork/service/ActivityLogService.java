package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.ActivityLog;
import com.example.mynewwork.repository.ActivityLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public Page<ActivityLog> getActivityLogs(Pageable pageable) {
        return activityLogRepository.findByOrderByCreatedAtDesc(pageable);
    }

    public List<ActivityLog> getRecentActivities(int limit) {
        return activityLogRepository.findByOrderByCreatedAtDesc(Pageable.ofSize(limit)).getContent();
    }

    public List<ActivityLog> findRecentActivities(int limit) {
        return activityLogRepository.findByOrderByCreatedAtDesc(Pageable.ofSize(limit)).getContent();
    }

    @Transactional
    public void createActivityLog(ActivityLog activityLog) {
        if (activityLog.getCreatedAt() == null) {
            activityLog.setCreatedAt(LocalDateTime.now());
        }
        activityLogRepository.save(activityLog);
    }

    public List<ActivityLog> getActivitiesByUser(Long userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ActivityLog> getActivitiesByModule(String module) {
        return activityLogRepository.findByModuleOrderByCreatedAtDesc(module);
    }

    @Transactional
    public void log(Long userId, String username, String operation, String module, 
                    String description, Object details, HttpServletRequest request) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setUserId(userId);
        activityLog.setUsername(username);
        activityLog.setOperation(operation);
        activityLog.setModule(module);
        activityLog.setDescription(description);
        activityLog.setSuccess(true);
        
        if (details != null) {
            try {
                activityLog.setDetails(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.warn("Failed to serialize activity details: {}", e.getMessage());
            }
        }
        
        if (request != null) {
            activityLog.setIpAddress(getClientIp(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
        }
        
        activityLogRepository.save(activityLog);
        log.debug("Activity logged: {} - {} - {}", username, operation, module);
    }

    @Transactional
    public void log(Long userId, String username, String operation, String module, String description) {
        log(userId, username, operation, module, description, null, null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
