package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.BusinessProcess;
import com.example.mynewwork.repository.BusinessProcessRepository;
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
 * 业务流程业务逻辑服务层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessProcessService {

    private final BusinessProcessRepository businessProcessRepository;

    /**
     * 根据ID查询业务流程
     *
     * @param id 流程ID
     * @return 业务流程信息
     */
    public Optional<BusinessProcess> findById(Long id) {
        log.debug("根据ID查询业务流程: {}", id);
        return businessProcessRepository.findByIdAndActiveTrue(id);
    }

    /**
     * 分页查询所有活跃的业务流程
     *
     * @param pageable 分页参数
     * @return 业务流程分页数据
     */
    public Page<BusinessProcess> findAll(Pageable pageable) {
        return findAll(null, null, pageable);
    }

    public Page<BusinessProcess> findAll(String category, String environmentType, Pageable pageable) {
        log.debug("分页查询业务流程, category={}, environmentType={}", category, environmentType);
        return businessProcessRepository.findFiltered(category, environmentType, pageable);
    }

    /**
     * 查询所有活跃的业务流程
     *
     * @return 活跃的业务流程列表
     */
    public List<BusinessProcess> findAllActive() {
        log.debug("查询所有活跃的业务流程");
        return businessProcessRepository.findByActiveTrue();
    }

    /**
     * 根据分类查询业务流程
     *
     * @param category 流程分类
     * @return 业务流程列表
     */
    public List<BusinessProcess> findByCategory(String category) {
        log.debug("根据分类查询业务流程: {}", category);
        return businessProcessRepository.findByCategory(category);
    }

    /**
     * 查询按优先级排序的业务流程
     *
     * @return 按优先级排序的业务流程列表
     */
    public List<BusinessProcess> findAllOrderByPriority() {
        log.debug("查询按优先级排序的业务流程");
        return businessProcessRepository.findByActiveTrueOrderByPriorityDesc();
    }

    /**
     * 根据关键字搜索业务流程
     *
     * @param keyword 搜索关键字
     * @return 匹配的业务流程列表
     */
    public List<BusinessProcess> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索业务流程: {}", keyword);
        return businessProcessRepository.findActiveByKeyword(keyword);
    }

    /**
     * 创建业务流程
     *
     * @param process 业务流程信息
     * @param userId 创建用户ID
     * @return 创建后的业务流程信息
     */
    @Transactional
    public BusinessProcess createProcess(BusinessProcess process, Long userId) {
        log.info("创建业务流程: {}", process.getName());

        // 检查流程名称是否已存在
        if (businessProcessRepository.existsByNameAndActiveTrue(process.getName())) {
            throw new DuplicateEntityException("业务流程", "名称", process.getName());
        }

        process.setCreatedBy(userId);
        process.setCreatedAt(LocalDateTime.now());
        process.setUpdatedAt(LocalDateTime.now());
        process.setActive(true);

        return businessProcessRepository.save(process);
    }

    /**
     * 更新业务流程
     *
     * @param id 流程ID
     * @param processDetails 流程详细信息
     * @param userId 更新用户ID
     * @return 更新后的业务流程信息
     */
    @Transactional
    public BusinessProcess updateProcess(Long id, BusinessProcess processDetails, Long userId) {
        log.info("更新业务流程, ID: {}", id);

        BusinessProcess process = businessProcessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("业务流程", id));

        // 如果名称被修改，检查新名称是否已存在
        if (!process.getName().equals(processDetails.getName()) &&
            businessProcessRepository.existsByNameAndActiveTrue(processDetails.getName())) {
            throw new DuplicateEntityException("业务流程", "名称", processDetails.getName());
        }

        // 更新流程信息
        process.setName(processDetails.getName());
        process.setDescription(processDetails.getDescription());
        process.setCategory(processDetails.getCategory());
        process.setEnvironmentId(processDetails.getEnvironmentId());
        process.setEnvironmentType(processDetails.getEnvironmentType());
        process.setProcessFlow(processDetails.getProcessFlow());
        process.setFlowchartPath(processDetails.getFlowchartPath());
        process.setSteps(processDetails.getSteps());
        process.setPrecautions(processDetails.getPrecautions());
        process.setChecklist(processDetails.getChecklist());
        process.setRelatedDocuments(processDetails.getRelatedDocuments());
        process.setPriority(processDetails.getPriority());
        process.setUpdatedAt(LocalDateTime.now());

        return businessProcessRepository.save(process);
    }

    /**
     * 删除业务流程（软删除）
     *
     * @param id 流程ID
     */
    @Transactional
    public void deleteProcess(Long id) {
        log.info("删除业务流程, ID: {}", id);

        BusinessProcess process = businessProcessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("业务流程", id));

        process.setActive(false);
        process.setUpdatedAt(LocalDateTime.now());

        businessProcessRepository.save(process);
    }

    /**
     * 永久删除业务流程
     *
     * @param id 流程ID
     */
    @Transactional
    public void permanentlyDeleteProcess(Long id) {
        log.info("永久删除业务流程, ID: {}", id);

        if (!businessProcessRepository.existsById(id)) {
            throw new EntityNotFoundException("业务流程", id);
        }

        businessProcessRepository.deleteById(id);
    }

    /**
     * 检查流程名称是否存在
     *
     * @param name 流程名称
     * @return 是否存在
     */
    public boolean existsByName(String name) {
        return businessProcessRepository.existsByNameAndActiveTrue(name);
    }
}