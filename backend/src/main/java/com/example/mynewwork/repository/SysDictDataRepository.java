package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.SysDictData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典数据 Repository
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Repository
public interface SysDictDataRepository extends JpaRepository<SysDictData, Long> {

    List<SysDictData> findByTypeCodeOrderBySortOrderAsc(String typeCode);

    List<SysDictData> findByTypeCodeAndStatusTrueOrderBySortOrderAsc(String typeCode);

    boolean existsByTypeCodeAndDataValue(String typeCode, String dataValue);

    void deleteByTypeCode(String typeCode);
}
