package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.Environment;
import com.example.mynewwork.repository.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 环境配置业务逻辑服务层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    /**
     * 根据ID查询环境配置
     *
     * @param id 环境ID
     * @return 环境配置信息
     */
    public Optional<Environment> findById(Long id) {
        log.debug("根据ID查询环境配置: {}", id);
        return environmentRepository.findByIdAndActiveTrue(id);
    }

    /**
     * 分页查询所有活跃的环境配置
     *
     * @param pageable 分页参数
     * @return 环境配置分页数据
     */
    public Page<Environment> findAll(Pageable pageable) {
        return findAll(null, pageable);
    }

    public Page<Environment> findAll(String type, Pageable pageable) {
        log.debug("分页查询环境配置, type={}", type);
        return environmentRepository.findFiltered(type, pageable);
    }

    /**
     * 查询所有活跃的环境配置
     *
     * @return 活跃的环境配置列表
     */
    public List<Environment> findAllActive() {
        log.debug("查询所有活跃的环境配置");
        return environmentRepository.findByActiveTrue();
    }

    /**
     * 根据环境类型查询
     *
     * @param type 环境类型
     * @return 环境配置列表
     */
    public List<Environment> findByType(String type) {
        log.debug("根据类型查询环境配置: {}", type);
        return environmentRepository.findByType(type);
    }

    /**
     * 根据关键字搜索环境配置
     *
     * @param keyword 搜索关键字
     * @return 匹配的环境配置列表
     */
    public List<Environment> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索环境配置: {}", keyword);
        return environmentRepository.findActiveByKeyword(keyword);
    }

    /**
     * 创建环境配置
     *
     * @param environment 环境配置信息
     * @param userId 创建用户ID
     * @return 创建后的环境配置信息
     */
    @Transactional
    public Environment createEnvironment(Environment environment, Long userId) {
        log.info("创建环境配置: {}", environment.getName());

        // 检查环境名称是否已存在
        if (environmentRepository.existsByNameAndActiveTrue(environment.getName())) {
            throw new DuplicateEntityException("环境", "名称", environment.getName());
        }

        environment.setCreatedBy(userId);
        environment.setCreatedAt(LocalDateTime.now());
        environment.setUpdatedAt(LocalDateTime.now());
        environment.setActive(true);

        return environmentRepository.save(environment);
    }

    /**
     * 更新环境配置
     *
     * @param id 环境ID
     * @param environmentDetails 环境详细信息
     * @param userId 更新用户ID
     * @return 更新后的环境配置信息
     */
    @Transactional
    public Environment updateEnvironment(Long id, Environment environmentDetails, Long userId) {
        log.info("更新环境配置, ID: {}", id);

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("环境", id));

        // 如果名称被修改，检查新名称是否已存在
        if (!environment.getName().equals(environmentDetails.getName()) &&
            environmentRepository.existsByNameAndActiveTrue(environmentDetails.getName())) {
            throw new DuplicateEntityException("环境", "名称", environmentDetails.getName());
        }

        // 更新环境信息
        environment.setName(environmentDetails.getName());
        environment.setDescription(environmentDetails.getDescription());
        environment.setType(environmentDetails.getType());
        environment.setUrl(environmentDetails.getUrl());
        environment.setUsername(environmentDetails.getUsername());
        environment.setPassword(environmentDetails.getPassword());
        environment.setDatabaseUrl(environmentDetails.getDatabaseUrl());
        environment.setDatabaseUsername(environmentDetails.getDatabaseUsername());
        environment.setDatabasePassword(environmentDetails.getDatabasePassword());
        environment.setRedisUrl(environmentDetails.getRedisUrl());
        environment.setRedisPassword(environmentDetails.getRedisPassword());
        environment.setMqUrl(environmentDetails.getMqUrl());
        environment.setMqUsername(environmentDetails.getMqUsername());
        environment.setMqPassword(environmentDetails.getMqPassword());
        environment.setNotes(environmentDetails.getNotes());
        environment.setUpdatedAt(LocalDateTime.now());

        return environmentRepository.save(environment);
    }

    /**
     * 删除环境配置（软删除）
     *
     * @param id 环境ID
     */
    @Transactional
    public void deleteEnvironment(Long id) {
        log.info("删除环境配置, ID: {}", id);

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("环境", id));

        environment.setActive(false);
        environment.setUpdatedAt(LocalDateTime.now());

        environmentRepository.save(environment);
    }

    /**
     * 永久删除环境配置
     *
     * @param id 环境ID
     */
    @Transactional
    public void permanentlyDeleteEnvironment(Long id) {
        log.info("永久删除环境配置, ID: {}", id);

        if (!environmentRepository.existsById(id)) {
            throw new EntityNotFoundException("环境", id);
        }

        environmentRepository.deleteById(id);
    }

    /**
     * 检查环境名称是否存在
     *
     * @param name 环境名称
     * @return 是否存在
     */
    public boolean existsByName(String name) {
        return environmentRepository.existsByNameAndActiveTrue(name);
    }
}