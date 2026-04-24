package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 环境数据访问层
 *
 * 提供环境实体的数据库操作接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * 根据类型查找环境列表
     *
     * @param type 环境类型
     * @return 环境列表
     */
    List<Environment> findByType(String type);

    /**
     * 查找所有启用的环境
     *
     * @return 启用的环境列表
     */
    List<Environment> findByActiveTrue();

    /**
     * 根据ID查找启用的环境
     *
     * @param id 环境ID
     * @return 环境信息
     */
    Optional<Environment> findByIdAndActiveTrue(Long id);

    /**
     * 根据关键字搜索启用的环境
     *
     * @param keyword 关键字（匹配名称、描述或类型）
     * @return 匹配的环境列表
     */
    @Query("SELECT e FROM Environment e WHERE e.active = true AND (e.name LIKE %:keyword% OR e.description LIKE %:keyword% OR e.type LIKE %:keyword%)")
    List<Environment> findActiveByKeyword(@Param("keyword") String keyword);

    /**
     * 检查名称是否已存在（启用的环境）
     *
     * @param name 环境名称
     * @return 是否存在
     */
    boolean existsByNameAndActiveTrue(String name);

    /**
     * 分页查询活跃的环境
     *
     * @param pageable 分页参数
     * @return 环境分页数据
     */
    Page<Environment> findByActiveTrue(Pageable pageable);

    /**
     * 根据可选类型分页查询活跃的环境
     */
    @Query("SELECT e FROM Environment e WHERE e.active = true " +
           "AND (:type IS NULL OR e.type = :type)")
    Page<Environment> findFiltered(@Param("type") String type, Pageable pageable);

    /**
     * 统计启用的环境数量
     *
     * @return 启用的环境数量
     */
    long countByActiveTrue();
}