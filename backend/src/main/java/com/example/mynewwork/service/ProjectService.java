package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.Project;
import com.example.mynewwork.repository.ProjectRepository;
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
 * 项目信息业务逻辑服务层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * 根据ID查询项目
     *
     * @param id 项目ID
     * @return 项目信息
     */
    public Optional<Project> findById(Long id) {
        log.debug("根据ID查询项目: {}", id);
        return projectRepository.findByIdAndActiveTrue(id);
    }

    /**
     * 分页查询所有活跃的项目
     *
     * @param pageable 分页参数
     * @return 项目分页数据
     */
    public Page<Project> findAll(Pageable pageable) {
        return findAll(null, pageable);
    }

    public Page<Project> findAll(String status, Pageable pageable) {
        log.debug("分页查询项目, status={}", status);
        return projectRepository.findFiltered(status, pageable);
    }

    /**
     * 查询所有活跃的项目
     *
     * @return 活跃的项目列表
     */
    public List<Project> findAllActive() {
        log.debug("查询所有活跃的项目");
        return projectRepository.findByActiveTrue();
    }

    /**
     * 根据项目全称查询项目
     *
     * @param projectFullName 项目全称
     * @return 项目列表
     */
    public List<Project> findByProjectFullName(String projectFullName) {
        log.debug("根据项目全称查询项目: {}", projectFullName);
        return projectRepository.findByProjectFullNameAndActiveTrue(projectFullName);
    }

    /**
     * 根据关键字搜索项目
     *
     * @param keyword 搜索关键字
     * @return 匹配的项目列表
     */
    public List<Project> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索项目: {}", keyword);
        return projectRepository.findActiveByKeyword(keyword);
    }

    /**
     * 创建项目
     *
     * @param project 项目信息
     * @param userId 创建用户ID
     * @return 创建后的项目信息
     */
    @Transactional
    public Project createProject(Project project, Long userId) {
        log.info("创建项目: {}", project.getName());

        // 检查项目名称是否已存在
        if (projectRepository.existsByNameAndActiveTrue(project.getName())) {
            throw new DuplicateEntityException("项目", "名称", project.getName());
        }

        project.setCreatedBy(userId);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setActive(true);

        return projectRepository.save(project);
    }

    /**
     * 更新项目信息
     *
     * @param id 项目ID
     * @param projectDetails 项目详细信息
     * @param userId 更新用户ID
     * @return 更新后的项目信息
     */
    @Transactional
    public Project updateProject(Long id, Project projectDetails, Long userId) {
        log.info("更新项目信息, ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("项目", id));

        // 如果名称被修改，检查新名称是否已存在
        if (!project.getName().equals(projectDetails.getName()) &&
            projectRepository.existsByNameAndActiveTrue(projectDetails.getName())) {
            throw new DuplicateEntityException("项目", "名称", projectDetails.getName());
        }

        // 更新项目信息
        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());
        project.setCodeRepository(projectDetails.getCodeRepository());
        project.setDocumentPath(projectDetails.getDocumentPath());
        project.setDeploymentPath(projectDetails.getDeploymentPath());
        project.setPort(projectDetails.getPort());
        project.setTeamMembers(projectDetails.getTeamMembers());
        project.setProjectFullName(projectDetails.getProjectFullName());
        project.setStatus(projectDetails.getStatus());
        project.setNotes(projectDetails.getNotes());
        project.setUpdatedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

    /**
     * 删除项目（软删除）
     *
     * @param id 项目ID
     */
    @Transactional
    public void deleteProject(Long id) {
        log.info("删除项目, ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("项目", id));

        project.setActive(false);
        project.setUpdatedAt(LocalDateTime.now());

        projectRepository.save(project);
    }

    /**
     * 永久删除项目
     *
     * @param id 项目ID
     */
    @Transactional
    public void permanentlyDeleteProject(Long id) {
        log.info("永久删除项目, ID: {}", id);

        if (!projectRepository.existsById(id)) {
            throw new EntityNotFoundException("项目", id);
        }

        projectRepository.deleteById(id);
    }

    /**
     * 检查项目名称是否存在
     *
     * @param name 项目名称
     * @return 是否存在
     */
    public boolean existsByName(String name) {
        return projectRepository.existsByNameAndActiveTrue(name);
    }
}