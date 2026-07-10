package com.example.mynewwork.model.dto;

import lombok.Data;

/**
 * 仪表盘统计数据 DTO
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
public class DashboardStats {
    private long environments;
    private long components;
    private long processes;
    private long projects;
    private long iterations;
}
