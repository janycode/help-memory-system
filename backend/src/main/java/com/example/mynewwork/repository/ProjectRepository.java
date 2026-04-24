package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目信息数据访问层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 查询活跃的项目
     *
     * @return 活跃的项目列表
     */
    List<Project> findByActiveTrue();

    /**
     * 根据ID查询活跃的项目
     *
     * @param id 项目ID
     * @return 项目信息
     */
    Optional<Project> findByIdAndActiveTrue(Long id);

    /**
     * 根据关键字搜索活跃的项目
     *
     * @param keyword 搜索关键字
     * @return 匹配的项目列表
     */
    @Query("SELECT p FROM Project p WHERE p.active = true AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword% OR p.projectFullName LIKE %:keyword%)")
    List<Project> findActiveByKeyword(@Param("keyword") String keyword);

    /**
     * 检查指定名称的活跃项目是否存在
     *
     * @param name 项目名称
     * @return 是否存在
     */
    boolean existsByNameAndActiveTrue(String name);

    /**
     * 根据项目全称查询项目
     *
     * @param projectFullName 项目全称
     * @return 项目列表
     */
    List<Project> findByProjectFullNameAndActiveTrue(String projectFullName);

    /**
     * 分页查询活跃的项目
     *
     * @param pageable 分页参数
     * @return 项目分页数据
     */
    Page<Project> findByActiveTrue(Pageable pageable);

    /**
     * 根据可选状态分页查询活跃的项目
     */
    @Query("SELECT p FROM Project p WHERE p.active = true " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Project> findFiltered(@Param("status") String status, Pageable pageable);

    /**
     * 统计活跃的项目数量
     *
     * @return 活跃的项目数量
     */
    long countByActiveTrue();
}