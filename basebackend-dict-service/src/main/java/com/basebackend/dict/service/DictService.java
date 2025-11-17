package com.basebackend.dict.service;

import com.basebackend.common.model.PageResult;
import com.basebackend.dict.dto.DictDTO;
import com.basebackend.dict.dto.DictDataDTO;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author BaseBackend Team
 */
public interface DictService {

    /**
     * 分页查询字典列表
     *
     * @param current  当前页
     * @param size     每页大小
     * @param dictName 字典名称
     * @param dictType 字典类型
     * @param status   状态
     * @return 分页结果
     */
    PageResult<DictDTO> getDictPage(Integer current, Integer size, String dictName, String dictType, Integer status);

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典DTO
     */
    DictDTO getDictById(Long id);

    /**
     * 创建字典
     *
     * @param dictDTO 字典DTO
     */
    void createDict(DictDTO dictDTO);

    /**
     * 更新字典
     *
     * @param id      字典ID
     * @param dictDTO 字典DTO
     */
    void updateDict(Long id, DictDTO dictDTO);

    /**
     * 删除字典
     *
     * @param id 字典ID
     */
    void deleteDict(Long id);

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<DictDataDTO> getDictDataByType(String dictType);

    /**
     * 分页查询字典数据列表
     *
     * @param current   当前页
     * @param size      每页大小
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param status    状态
     * @return 分页结果
     */
    PageResult<DictDataDTO> getDictDataPage(Integer current, Integer size, String dictType, String dictLabel, Integer status);

    /**
     * 根据ID查询字典数据
     *
     * @param id 字典数据ID
     * @return 字典数据DTO
     */
    DictDataDTO getDictDataById(Long id);

    /**
     * 创建字典数据
     *
     * @param dictDataDTO 字典数据DTO
     */
    void createDictData(DictDataDTO dictDataDTO);

    /**
     * 更新字典数据
     *
     * @param id          字典数据ID
     * @param dictDataDTO 字典数据DTO
     */
    void updateDictData(Long id, DictDataDTO dictDataDTO);

    /**
     * 删除字典数据
     *
     * @param id 字典数据ID
     */
    void deleteDictData(Long id);

    /**
     * 刷新字典缓存
     */
    void refreshCache();

    /**
     * 加载所有字典到缓存
     */
    void loadDictCache();
}
