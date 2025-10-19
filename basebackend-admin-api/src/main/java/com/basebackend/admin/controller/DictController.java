package com.basebackend.admin.controller;

import com.basebackend.admin.dto.DictDTO;
import com.basebackend.admin.dto.DictDataDTO;
import com.basebackend.admin.service.DictService;
import com.basebackend.common.model.PageResult;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 字典管理控制器
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/admin/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @Operation(summary = "分页查询字典列表")
    @GetMapping
    public Result<PageResult<DictDTO>> getDictPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) Integer status) {
        PageResult<DictDTO> page = dictService.getDictPage(current, size, dictName, dictType, status);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询字典")
    @GetMapping("/{id}")
    public Result<DictDTO> getDictById(@PathVariable Long id) {
        DictDTO dict = dictService.getDictById(id);
        return Result.success(dict);
    }

    @Operation(summary = "创建字典")
    @PostMapping
    public Result<String> createDict(@Valid @RequestBody DictDTO dictDTO) {
        dictService.createDict(dictDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新字典")
    @PutMapping("/{id}")
    public Result<String> updateDict(@PathVariable Long id, @Valid @RequestBody DictDTO dictDTO) {
        dictService.updateDict(id, dictDTO);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{id}")
    public Result<String> deleteDict(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "根据字典类型查询字典数据")
    @GetMapping("/data/type/{dictType}")
    public Result<List<DictDataDTO>> getDictDataByType(@PathVariable String dictType) {
        List<DictDataDTO> dataList = dictService.getDictDataByType(dictType);
        return Result.success(dataList);
    }

    @Operation(summary = "分页查询字典数据列表")
    @GetMapping("/data")
    public Result<PageResult<DictDataDTO>> getDictDataPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) Integer status) {
        PageResult<DictDataDTO> page = dictService.getDictDataPage(current, size, dictType, dictLabel, status);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询字典数据")
    @GetMapping("/data/{id}")
    public Result<DictDataDTO> getDictDataById(@PathVariable Long id) {
        DictDataDTO dictData = dictService.getDictDataById(id);
        return Result.success(dictData);
    }

    @Operation(summary = "创建字典数据")
    @PostMapping("/data")
    public Result<String> createDictData(@Valid @RequestBody DictDataDTO dictDataDTO) {
        dictService.createDictData(dictDataDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新字典数据")
    @PutMapping("/data/{id}")
    public Result<String> updateDictData(@PathVariable Long id, @Valid @RequestBody DictDataDTO dictDataDTO) {
        dictService.updateDictData(id, dictDataDTO);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public Result<String> deleteDictData(@PathVariable Long id) {
        dictService.deleteDictData(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "刷新字典缓存")
    @PostMapping("/refresh-cache")
    public Result<String> refreshCache() {
        dictService.refreshCache();
        return Result.success("缓存刷新成功");
    }
}