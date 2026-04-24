package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.SysDictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典类型 Repository
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface SysDictTypeRepository extends JpaRepository<SysDictType, Long> {

    Optional<SysDictType> findByTypeCode(String typeCode);

    boolean existsByTypeCode(String typeCode);

    List<SysDictType> findByStatusTrueOrderByCreatedAtDesc();

    List<SysDictType> findAllByOrderByCreatedAtDesc();
}
