package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.SysDictData;
import com.example.mynewwork.model.entity.SysDictType;
import com.example.mynewwork.repository.SysDictDataRepository;
import com.example.mynewwork.repository.SysDictTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典管理服务
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SysDictService {

    private final SysDictTypeRepository dictTypeRepository;
    private final SysDictDataRepository dictDataRepository;

    // ==================== 字典类型管理 ====================

    public List<SysDictType> getAllDictTypes() {
        return dictTypeRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SysDictType> getActiveDictTypes() {
        return dictTypeRepository.findByStatusTrueOrderByCreatedAtDesc();
    }

    public SysDictType getDictTypeById(Long id) {
        return dictTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("字典类型", id));
    }

    public SysDictType getDictTypeByCode(String typeCode) {
        return dictTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new EntityNotFoundException("字典类型", "编码", typeCode));
    }

    @Transactional
    public SysDictType createDictType(SysDictType dictType) {
        log.info("创建字典类型: {}", dictType.getTypeCode());

        if (dictTypeRepository.existsByTypeCode(dictType.getTypeCode())) {
            throw new DuplicateEntityException("字典类型", "编码", dictType.getTypeCode());
        }

        return dictTypeRepository.save(dictType);
    }

    @Transactional
    public SysDictType updateDictType(Long id, SysDictType dictTypeDetails) {
        log.info("更新字典类型: {}", id);

        SysDictType dictType = getDictTypeById(id);

        if (!dictType.getTypeCode().equals(dictTypeDetails.getTypeCode()) &&
            dictTypeRepository.existsByTypeCode(dictTypeDetails.getTypeCode())) {
            throw new DuplicateEntityException("字典类型", "编码", dictTypeDetails.getTypeCode());
        }

        dictType.setTypeCode(dictTypeDetails.getTypeCode());
        dictType.setTypeName(dictTypeDetails.getTypeName());
        dictType.setDescription(dictTypeDetails.getDescription());
        dictType.setStatus(dictTypeDetails.getStatus());

        return dictTypeRepository.save(dictType);
    }

    @Transactional
    public void deleteDictType(Long id) {
        log.info("删除字典类型: {}", id);

        SysDictType dictType = getDictTypeById(id);
        dictDataRepository.deleteByTypeCode(dictType.getTypeCode());
        dictTypeRepository.delete(dictType);
    }

    // ==================== 字典数据管理 ====================

    public List<SysDictData> getDictDataByTypeCode(String typeCode) {
        return dictDataRepository.findByTypeCodeAndStatusTrueOrderBySortOrderAsc(typeCode);
    }

    public List<SysDictData> getAllDictDataByTypeCode(String typeCode) {
        return dictDataRepository.findByTypeCodeOrderBySortOrderAsc(typeCode);
    }

    public SysDictData getDictDataById(Long id) {
        return dictDataRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("字典数据", id));
    }

    @Transactional
    public SysDictData createDictData(SysDictData dictData) {
        log.info("创建字典数据: {} - {}", dictData.getTypeCode(), dictData.getDataValue());

        if (dictDataRepository.existsByTypeCodeAndDataValue(dictData.getTypeCode(), dictData.getDataValue())) {
            throw new DuplicateEntityException("字典数据", "值", dictData.getDataValue());
        }

        return dictDataRepository.save(dictData);
    }

    @Transactional
    public SysDictData updateDictData(Long id, SysDictData dictDataDetails) {
        log.info("更新字典数据: {}", id);

        SysDictData dictData = getDictDataById(id);

        if (!dictData.getDataValue().equals(dictDataDetails.getDataValue()) &&
            dictDataRepository.existsByTypeCodeAndDataValue(dictData.getTypeCode(), dictDataDetails.getDataValue())) {
            throw new DuplicateEntityException("字典数据", "值", dictDataDetails.getDataValue());
        }

        dictData.setDataValue(dictDataDetails.getDataValue());
        dictData.setDataLabel(dictDataDetails.getDataLabel());
        dictData.setSortOrder(dictDataDetails.getSortOrder());
        dictData.setStatus(dictDataDetails.getStatus());
        dictData.setRemark(dictDataDetails.getRemark());

        return dictDataRepository.save(dictData);
    }

    @Transactional
    public void deleteDictData(Long id) {
        log.info("删除字典数据: {}", id);

        if (!dictDataRepository.existsById(id)) {
            throw new EntityNotFoundException("字典数据", id);
        }

        dictDataRepository.deleteById(id);
    }
}
