package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.SysDictData;
import com.example.mynewwork.model.entity.SysDictType;
import com.example.mynewwork.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 *
 * 处理字典类型和字典数据的增删改查操作
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
@Tag(name = "字典管理", description = "字典类型和字典数据管理接口")
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping("/types")
    @Operation(summary = "查询所有字典类型")
    public ResponseEntity<ApiResponse<List<SysDictType>>> getAllDictTypes() {
        List<SysDictType> dictTypes = sysDictService.getAllDictTypes();
        return ResponseEntity.ok(ApiResponse.success(dictTypes, "查询成功"));
    }

    @GetMapping("/types/active")
    @Operation(summary = "查询启用的字典类型")
    public ResponseEntity<ApiResponse<List<SysDictType>>> getActiveDictTypes() {
        List<SysDictType> dictTypes = sysDictService.getActiveDictTypes();
        return ResponseEntity.ok(ApiResponse.success(dictTypes, "查询成功"));
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "根据ID查询字典类型")
    public ResponseEntity<ApiResponse<SysDictType>> getDictType(@PathVariable Long id) {
        SysDictType dictType = sysDictService.getDictTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(dictType, "查询成功"));
    }

    @PostMapping("/types")
    @Operation(summary = "创建字典类型")
    public ResponseEntity<ApiResponse<SysDictType>> createDictType(@Valid @RequestBody SysDictType dictType) {
        log.info("创建字典类型: {}", dictType.getTypeCode());
        SysDictType created = sysDictService.createDictType(dictType);
        return ResponseEntity.ok(ApiResponse.success(created, "创建成功"));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "更新字典类型")
    public ResponseEntity<ApiResponse<SysDictType>> updateDictType(
            @PathVariable Long id, @Valid @RequestBody SysDictType dictType) {
        log.info("更新字典类型: {}", id);
        SysDictType updated = sysDictService.updateDictType(id, dictType);
        return ResponseEntity.ok(ApiResponse.success(updated, "更新成功"));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "删除字典类型")
    public ResponseEntity<ApiResponse<Void>> deleteDictType(@PathVariable Long id) {
        log.info("删除字典类型: {}", id);
        sysDictService.deleteDictType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @GetMapping("/data/{typeCode}")
    @Operation(summary = "根据类型编码查询字典数据")
    public ResponseEntity<ApiResponse<List<SysDictData>>> getDictDataByType(@PathVariable String typeCode) {
        List<SysDictData> dictData = sysDictService.getDictDataByTypeCode(typeCode);
        return ResponseEntity.ok(ApiResponse.success(dictData, "查询成功"));
    }

    @GetMapping("/data/{typeCode}/all")
    @Operation(summary = "根据类型编码查询所有字典数据（包含禁用的）")
    public ResponseEntity<ApiResponse<List<SysDictData>>> getAllDictDataByType(@PathVariable String typeCode) {
        List<SysDictData> dictData = sysDictService.getAllDictDataByTypeCode(typeCode);
        return ResponseEntity.ok(ApiResponse.success(dictData, "查询成功"));
    }

    @GetMapping("/data/id/{id}")
    @Operation(summary = "根据ID查询字典数据")
    public ResponseEntity<ApiResponse<SysDictData>> getDictDataById(@PathVariable Long id) {
        SysDictData dictData = sysDictService.getDictDataById(id);
        return ResponseEntity.ok(ApiResponse.success(dictData, "查询成功"));
    }

    @PostMapping("/data")
    @Operation(summary = "创建字典数据")
    public ResponseEntity<ApiResponse<SysDictData>> createDictData(@Valid @RequestBody SysDictData dictData) {
        log.info("创建字典数据: {} - {}", dictData.getTypeCode(), dictData.getDataValue());
        SysDictData created = sysDictService.createDictData(dictData);
        return ResponseEntity.ok(ApiResponse.success(created, "创建成功"));
    }

    @PutMapping("/data/id/{id}")
    @Operation(summary = "更新字典数据")
    public ResponseEntity<ApiResponse<SysDictData>> updateDictData(
            @PathVariable Long id, @Valid @RequestBody SysDictData dictData) {
        log.info("更新字典数据: {}", id);
        SysDictData updated = sysDictService.updateDictData(id, dictData);
        return ResponseEntity.ok(ApiResponse.success(updated, "更新成功"));
    }

    @DeleteMapping("/data/id/{id}")
    @Operation(summary = "删除字典数据")
    public ResponseEntity<ApiResponse<Void>> deleteDictData(@PathVariable Long id) {
        log.info("删除字典数据: {}", id);
        sysDictService.deleteDictData(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }
}
