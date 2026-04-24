package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.SystemConfig;
import com.example.mynewwork.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    public List<SystemConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    public Optional<SystemConfig> getConfigByKey(String key) {
        return configRepository.findByConfigKey(key);
    }

    public String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Transactional
    public SystemConfig setConfig(String key, String value, String description) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        return configRepository.save(config);
    }

    @Transactional
    public SystemConfig updateConfig(Long id, String value) {
        SystemConfig config = configRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("配置项不存在"));
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new RuntimeException("该配置项不可编辑");
        }
        config.setConfigValue(value);
        return configRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }
}
