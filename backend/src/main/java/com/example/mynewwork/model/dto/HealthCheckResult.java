package com.example.mynewwork.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HealthCheckResult {
    private boolean reachable;
    private long latencyMs;
    private String message;
}
