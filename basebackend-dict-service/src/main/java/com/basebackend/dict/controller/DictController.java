package com.basebackend.dict.controller;

import com.basebackend.common.model.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.dict.dto.DictDTO;
import com.basebackend.dict.dto.DictDataDTO;
import com.basebackend.dict.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 *
 * @author BaseBackend Team
 */
@Slf4j
@Tag(name = "字典管理", description = "字典类型和字典数据管理接口")
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
@Validated
public class DictController {

    private final DictService dictService;

    /**
     * 分页查询字典列表
     */
    @Operation(summary = "分页查询字典列表", description = "分页查询字典类型列表")
    @GetMapping
    public Result<PageResult<DictDTO>> getDictPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "字典名称") @RequestParam(required = false) String dictName,
            @Parameter(description = "字典类型") @RequestParam(required = false) String dictType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("分页查询字典列表: current={}, size={}", current, size);
        try {
            PageResult<DictDTO> page = dictService.getDictPage(current, size, dictName, dictType, status);
            return Result.success(page);
        } catch (Exception e) {
            log.error("分页查询字典列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询字典
     */
    @Operation(summary = "根据ID查询字典", description = "根据ID查询字典详情")
    @GetMapping("/{id}")
    public Result<DictDTO> getDictById(@Parameter(description = "字典ID") @PathVariable Long id) {
        log.info("根据ID查询字典: {}", id);
        try {
            DictDTO dict = dictService.getDictById(id);
            return Result.success(dict);
        } catch (Exception e) {
            log.error("根据ID查询字典失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建字典
     */
    @Operation(summary = "创建字典", description = "创建新的字典类型")
    @PostMapping
    public Result<String> createDict(@Valid @RequestBody DictDTO dictDTO) {
        log.info("创建字典: {}", dictDTO.getDictName());
        try {
            dictService.createDict(dictDTO);
            return Result.success("创建成功");
        } catch (Exception e) {
            log.error("创建字典失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新字典
     */
    @Operation(summary = "更新字典", description = "更新字典类型信息")
    @PutMapping("/{id}")
    public Result<String> updateDict(
            @Parameter(description = "字典ID") @PathVariable Long id,
            @Valid @RequestBody DictDTO dictDTO) {
        log.info("更新字典: {}", id);
        try {
            dictService.updateDict(id, dictDTO);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新字典失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除字典
     */
    @Operation(summary = "删除字典", description = "删除字典类型（会级联删除所有关联的字典数据）")
    @DeleteMapping("/{id}")
    public Result<String> deleteDict(@Parameter(description = "字典ID") @PathVariable Long id) {
        log.info("删除字典: {}", id);
        try {
            dictService.deleteDict(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除字典失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据字典类型查询字典数据
     */
    @Operation(summary = "根据字典类型查询字典数据", description = "根据字典类型查询字典数据列表（用于下拉框等场景，优先从缓存读取）")
    @GetMapping("/data/type/{dictType}")
    public Result<List<DictDataDTO>> getDictDataByType(
            @Parameter(description = "字典类型", example = "user_status") @PathVariable String dictType) {
        log.info("根据字典类型查询字典数据: {}", dictType);
        try {
            List<DictDataDTO> dataList = dictService.getDictDataByType(dictType);
            return Result.success(dataList);
        } catch (Exception e) {
            log.error("根据字典类型查询字典数据失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询字典数据列表
     */
    @Operation(summary = "分页查询字典数据列表", description = "分页查询字典数据列表")
    @GetMapping("/data")
    public Result<PageResult<DictDataDTO>> getDictDataPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "字典类型") @RequestParam(required = false) String dictType,
            @Parameter(description = "字典标签") @RequestParam(required = false) String dictLabel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("分页查询字典数据列表: current={}, size={}", current, size);
        try {
            PageResult<DictDataDTO> page = dictService.getDictDataPage(current, size, dictType, dictLabel, status);
            return Result.success(page);
        } catch (Exception e) {
            log.error("分页查询字典数据列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询字典数据
     */
    @Operation(summary = "根据ID查询字典数据", description = "根据ID查询字典数据详情")
    @GetMapping("/data/{id}")
    public Result<DictDataDTO> getDictDataById(@Parameter(description = "字典数据ID") @PathVariable Long id) {
        log.info("根据ID查询字典数据: {}", id);
        try {
            DictDataDTO dictData = dictService.getDictDataById(id);
            return Result.success(dictData);
        } catch (Exception e) {
            log.error("根据ID查询字典数据失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建字典数据
     */
    @Operation(summary = "创建字典数据", description = "创建新的字典数据")
    @PostMapping("/data")
    public Result<String> createDictData(@Valid @RequestBody DictDataDTO dictDataDTO) {
        log.info("创建字典数据: {}", dictDataDTO.getDictLabel());
        try {
            dictService.createDictData(dictDataDTO);
            return Result.success("创建成功");
        } catch (Exception e) {
            log.error("创建字典数据失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新字典数据
     */
    @Operation(summary = "更新字典数据", description = "更新字典数据信息")
    @PutMapping("/data/{id}")
    public Result<String> updateDictData(
            @Parameter(description = "字典数据ID") @PathVariable Long id,
            @Valid @RequestBody DictDataDTO dictDataDTO) {
        log.info("更新字典数据: {}", id);
        try {
            dictService.updateDictData(id, dictDataDTO);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新字典数据失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除字典数据
     */
    @Operation(summary = "删除字典数据", description = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public Result<String> deleteDictData(@Parameter(description = "字典数据ID") @PathVariable Long id) {
        log.info("删除字典数据: {}", id);
        try {
            dictService.deleteDictData(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除字典数据失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新字典缓存
     */
    @Operation(summary = "刷新字典缓存", description = "手动刷新所有字典数据缓存")
    @PostMapping("/refresh-cache")
    public Result<String> refreshCache() {
        log.info("刷新字典缓存");
        try {
            dictService.refreshCache();
            return Result.success("缓存刷新成功");
        } catch (Exception e) {
            log.error("刷新字典缓存失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
