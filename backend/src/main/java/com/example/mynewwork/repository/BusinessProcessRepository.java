package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.BusinessProcess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业务流程数据访问层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, Long> {

    /**
     * 根据分类查询业务流程
     *
     * @param category 流程分类
     * @return 业务流程列表
     */
    List<BusinessProcess> findByCategory(String category);

    /**
     * 查询活跃的业务流程
     *
     * @return 活跃的业务流程列表
     */
    List<BusinessProcess> findByActiveTrue();

    /**
     * 根据ID查询活跃的业务流程
     *
     * @param id 流程ID
     * @return 业务流程
     */
    Optional<BusinessProcess> findByIdAndActiveTrue(Long id);

    /**
     * 根据关键字搜索活跃的业务流程
     *
     * @param keyword 搜索关键字
     * @return 匹配的业务流程列表
     */
    @Query("SELECT b FROM BusinessProcess b WHERE b.active = true AND (b.name LIKE %:keyword% OR b.description LIKE %:keyword% OR b.category LIKE %:keyword%)")
    List<BusinessProcess> findActiveByKeyword(@Param("keyword") String keyword);

    /**
     * 检查指定名称的活跃流程是否存在
     *
     * @param name 流程名称
     * @return 是否存在
     */
    boolean existsByNameAndActiveTrue(String name);

    /**
     * 根据优先级排序查询活跃的业务流程
     *
     * @return 按优先级排序的业务流程列表
     */
    List<BusinessProcess> findByActiveTrueOrderByPriorityDesc();

    /**
     * 分页查询活跃的业务流程
     *
     * @param pageable 分页参数
     * @return 业务流程分页数据
     */
    Page<BusinessProcess> findByActiveTrue(Pageable pageable);

    /**
     * 根据可选条件分页查询活跃的业务流程
     */
    @Query("SELECT b FROM BusinessProcess b WHERE b.active = true " +
           "AND (:category IS NULL OR b.category = :category) " +
           "AND (:environmentType IS NULL OR b.environmentType = :environmentType)")
    Page<BusinessProcess> findFiltered(@Param("category") String category,
                                       @Param("environmentType") String environmentType,
                                       Pageable pageable);

    /**
     * 统计活跃的业务流程数量
     *
     * @return 活跃的业务流程数量
     */
    long countByActiveTrue();
}