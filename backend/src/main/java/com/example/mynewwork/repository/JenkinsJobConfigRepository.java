package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.JenkinsJobConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Jenkins 监控配置仓库
 *
 * @author jiangyuan
 * @since 1.0.0
 */
public interface JenkinsJobConfigRepository extends JpaRepository<JenkinsJobConfig, Long> {

    /**
     * 查询所有已启用的监控配置
     */
    List<JenkinsJobConfig> findByEnabledTrue();

    /**
     * 判断指定 job 是否存在监控配置
     */
    boolean existsByJobName(String jobName);
}