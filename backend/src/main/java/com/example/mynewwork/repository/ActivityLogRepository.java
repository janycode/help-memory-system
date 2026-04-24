package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动日志数据访问层
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * 根据用户ID查询活动日志
     */
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 分页查询活动日志
     */
    Page<ActivityLog> findByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据操作模块查询活动日志
     */
    List<ActivityLog> findByModuleOrderByCreatedAtDesc(String module);

    /**
     * 查询指定时间范围内的活动日志
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime ORDER BY a.createdAt DESC")
    List<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的N条活动日志
     */
    List<ActivityLog> findTop10ByOrderByCreatedAtDesc();

    /**
     * 根据操作类型查询活动日志
     */
    List<ActivityLog> findByOperationOrderByCreatedAtDesc(String operation);

    /**
     * 统计指定用户的活动数量
     */
    long countByUserId(Long userId);

    /**
     * 删除指定时间之前的日志
     */
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}