package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.TechnicalComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 技术组件数据访问层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface TechnicalComponentRepository extends JpaRepository<TechnicalComponent, Long> {

    /**
     * 根据分类查询技术组件
     *
     * @param category 组件分类
     * @return 技术组件列表
     */
    List<TechnicalComponent> findByCategory(String category);

    /**
     * 查询活跃的技术组件
     *
     * @return 活跃的技术组件列表
     */
    List<TechnicalComponent> findByActiveTrue();

    /**
     * 根据ID查询活跃的技术组件
     *
     * @param id 组件ID
     * @return 技术组件
     */
    Optional<TechnicalComponent> findByIdAndActiveTrue(Long id);

    /**
     * 根据关键字搜索活跃的技术组件
     *
     * @param keyword 搜索关键字
     * @return 匹配的技术组件列表
     */
    @Query("SELECT t FROM TechnicalComponent t WHERE t.active = true AND (t.name LIKE %:keyword% OR t.description LIKE %:keyword% OR t.category LIKE %:keyword%)")
    List<TechnicalComponent> findActiveByKeyword(@Param("keyword") String keyword);

    /**
     * 检查指定名称的活跃组件是否存在
     *
     * @param name 组件名称
     * @return 是否存在
     */
    boolean existsByNameAndActiveTrue(String name);

    /**
     * 根据环境ID查询技术组件
     *
     * @param environmentId 环境ID
     * @return 技术组件列表
     */
    List<TechnicalComponent> findByEnvironmentIdAndActiveTrue(Long environmentId);

    /**
     * 分页查询活跃的技术组件
     *
     * @param pageable 分页参数
     * @return 技术组件分页数据
     */
    Page<TechnicalComponent> findByActiveTrue(Pageable pageable);

    /**
     * 根据可选条件分页查询活跃的技术组件
     */
    @Query("SELECT t FROM TechnicalComponent t WHERE t.active = true " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:environmentType IS NULL OR t.environmentType = :environmentType)")
    Page<TechnicalComponent> findFiltered(@Param("category") String category,
                                          @Param("environmentType") String environmentType,
                                          Pageable pageable);

    /**
     * 统计活跃的技术组件数量
     *
     * @return 活跃的技术组件数量
     */
    long countByActiveTrue();
}