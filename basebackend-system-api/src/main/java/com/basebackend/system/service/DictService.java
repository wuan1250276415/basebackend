package com.basebackend.system.service;

import com.basebackend.common.model.PageResult;
import com.basebackend.system.dto.DictDTO;
import com.basebackend.system.dto.DictDataDTO;

import java.util.List;

/**
 * 字典服务接口
 */
public interface DictService {

    /**
     * 分页查询字典列表
     */
    PageResult<DictDTO> getDictPage(Integer current, Integer size, String dictName, String dictType, Integer status);

    /**
     * 根据ID查询字典
     */
    DictDTO getDictById(Long id);

    /**
     * 创建字典
     */
    void createDict(DictDTO dictDTO);

    /**
     * 更新字典
     */
    void updateDict(Long id, DictDTO dictDTO);

    /**
     * 删除字典
     */
    void deleteDict(Long id);

    /**
     * 批量删除字典
     */
    void deleteDictBatch(List<Long> ids);

    /**
     * 根据字典类型查询字典数据
     */
    List<DictDataDTO> getDictDataByType(String dictType);

    /**
     * 分页查询字典数据列表
     */
    PageResult<DictDataDTO> getDictDataPage(Integer current, Integer size, String dictType, String dictLabel, Integer status);

    /**
     * 根据ID查询字典数据
     */
    DictDataDTO getDictDataById(Long id);

    /**
     * 创建字典数据
     */
    void createDictData(DictDataDTO dictDataDTO);

    /**
     * 更新字典数据
     */
    void updateDictData(Long id, DictDataDTO dictDataDTO);

    /**
     * 删除字典数据
     */
    void deleteDictData(Long id);

    /**
     * 批量删除字典数据
     */
    void deleteDictDataBatch(List<Long> ids);
}
