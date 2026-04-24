package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.dto.HealthCheckResult;
import com.example.mynewwork.model.entity.TechnicalComponent;
import com.example.mynewwork.repository.TechnicalComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 技术组件业务逻辑服务层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechnicalComponentService {

    private final TechnicalComponentRepository technicalComponentRepository;

    /**
     * 根据ID查询技术组件
     *
     * @param id 组件ID
     * @return 技术组件信息
     */
    public Optional<TechnicalComponent> findById(Long id) {
        log.debug("根据ID查询技术组件: {}", id);
        return technicalComponentRepository.findByIdAndActiveTrue(id);
    }

    /**
     * 分页查询所有活跃的技术组件
     *
     * @param pageable 分页参数
     * @return 技术组件分页数据
     */
    public Page<TechnicalComponent> findAll(Pageable pageable) {
        return findAll(null, null, pageable);
    }

    public Page<TechnicalComponent> findAll(String category, String environmentType, Pageable pageable) {
        log.debug("分页查询技术组件, category={}, environmentType={}", category, environmentType);
        return technicalComponentRepository.findFiltered(category, environmentType, pageable);
    }

    /**
     * 查询所有活跃的技术组件
     *
     * @return 活跃的技术组件列表
     */
    public List<TechnicalComponent> findAllActive() {
        log.debug("查询所有活跃的技术组件");
        return technicalComponentRepository.findByActiveTrue();
    }

    /**
     * 根据分类查询技术组件
     *
     * @param category 组件分类
     * @return 技术组件列表
     */
    public List<TechnicalComponent> findByCategory(String category) {
        log.debug("根据分类查询技术组件: {}", category);
        return technicalComponentRepository.findByCategory(category);
    }

    /**
     * 根据环境ID查询技术组件
     *
     * @param environmentId 环境ID
     * @return 技术组件列表
     */
    public List<TechnicalComponent> findByEnvironmentId(Long environmentId) {
        log.debug("根据环境ID查询技术组件: {}", environmentId);
        return technicalComponentRepository.findByEnvironmentIdAndActiveTrue(environmentId);
    }

    /**
     * 根据关键字搜索技术组件
     *
     * @param keyword 搜索关键字
     * @return 匹配的技术组件列表
     */
    public List<TechnicalComponent> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索技术组件: {}", keyword);
        return technicalComponentRepository.findActiveByKeyword(keyword);
    }

    /**
     * 创建技术组件
     *
     * @param component 技术组件信息
     * @param userId 创建用户ID
     * @return 创建后的技术组件信息
     */
    @Transactional
    public TechnicalComponent createComponent(TechnicalComponent component, Long userId) {
        log.info("创建技术组件: {}", component.getName());

        // 检查组件名称是否已存在
        if (technicalComponentRepository.existsByNameAndActiveTrue(component.getName())) {
            throw new DuplicateEntityException("技术组件", "名称", component.getName());
        }

        component.setCreatedBy(userId);
        component.setCreatedAt(LocalDateTime.now());
        component.setUpdatedAt(LocalDateTime.now());
        component.setActive(true);

        return technicalComponentRepository.save(component);
    }

    /**
     * 更新技术组件
     *
     * @param id 组件ID
     * @param componentDetails 组件详细信息
     * @param userId 更新用户ID
     * @return 更新后的技术组件信息
     */
    @Transactional
    public TechnicalComponent updateComponent(Long id, TechnicalComponent componentDetails, Long userId) {
        log.info("更新技术组件, ID: {}", id);

        TechnicalComponent component = technicalComponentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("技术组件", id));

        // 如果名称被修改，检查新名称是否已存在
        if (!component.getName().equals(componentDetails.getName()) &&
            technicalComponentRepository.existsByNameAndActiveTrue(componentDetails.getName())) {
            throw new DuplicateEntityException("技术组件", "名称", componentDetails.getName());
        }

        // 更新组件信息
        component.setName(componentDetails.getName());
        component.setDescription(componentDetails.getDescription());
        component.setCategory(componentDetails.getCategory());
        component.setUrl(componentDetails.getUrl());
        component.setUsername(componentDetails.getUsername());
        component.setPassword(componentDetails.getPassword());
        component.setVersion(componentDetails.getVersion());
        component.setEnvironmentId(componentDetails.getEnvironmentId());
        component.setEnvironmentType(componentDetails.getEnvironmentType());
        component.setConfiguration(componentDetails.getConfiguration());
        component.setNotes(componentDetails.getNotes());
        component.setUpdatedAt(LocalDateTime.now());

        return technicalComponentRepository.save(component);
    }

    /**
     * 检查技术组件连通性
     *
     * @param id 组件ID
     * @return 连通性检查结果
     */
    public HealthCheckResult checkHealth(Long id) {
        TechnicalComponent component = technicalComponentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("技术组件", id));

        String url = component.getUrl();
        if (url == null || url.isBlank()) {
            return new HealthCheckResult(false, 0, "未配置访问地址");
        }

        try {
            String host;
            int port;

            // parse host:port from URL
            if (url.startsWith("http://") || url.startsWith("https://")) {
                URI uri = new URI(url);
                host = uri.getHost();
                port = uri.getPort();
                if (port <= 0) port = url.startsWith("https") ? 443 : 80;
            } else if (url.contains("://")) {
                URI uri = new URI(url);
                host = uri.getHost();
                port = uri.getPort();
                if (port <= 0) port = 80;
            } else {
                // plain host:port
                String[] parts = url.split(":");
                host = parts[0].trim();
                port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 80;
            }

            long start = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                long elapsed = System.currentTimeMillis() - start;
                return new HealthCheckResult(true, elapsed, host + ":" + port + " 可达");
            }
        } catch (Exception e) {
            log.warn("健康检查失败, componentId={}, error={}", id, e.getMessage());
            return new HealthCheckResult(false, 0, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 删除技术组件（软删除）
     *
     * @param id 组件ID
     */
    @Transactional
    public void deleteComponent(Long id) {
        log.info("删除技术组件, ID: {}", id);

        TechnicalComponent component = technicalComponentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("技术组件", id));

        component.setActive(false);
        component.setUpdatedAt(LocalDateTime.now());

        technicalComponentRepository.save(component);
    }

    /**
     * 永久删除技术组件
     *
     * @param id 组件ID
     */
    @Transactional
    public void permanentlyDeleteComponent(Long id) {
        log.info("永久删除技术组件, ID: {}", id);

        if (!technicalComponentRepository.existsById(id)) {
            throw new EntityNotFoundException("技术组件", id);
        }

        technicalComponentRepository.deleteById(id);
    }

    /**
     * 检查组件名称是否存在
     *
     * @param name 组件名称
     * @return 是否存在
     */
    public boolean existsByName(String name) {
        return technicalComponentRepository.existsByNameAndActiveTrue(name);
    }
}