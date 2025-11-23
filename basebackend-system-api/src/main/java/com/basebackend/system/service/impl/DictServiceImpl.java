package com.basebackend.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.system.dto.DictDTO;
import com.basebackend.system.dto.DictDataDTO;
import com.basebackend.system.entity.SysDict;
import com.basebackend.system.entity.SysDictData;
import com.basebackend.system.mapper.SysDictDataMapper;
import com.basebackend.system.mapper.SysDictMapper;
import com.basebackend.system.service.DictService;
import com.basebackend.cache.service.RedisService;
import com.basebackend.common.model.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典服务实现
 */
@Slf4j
@Service
public class DictServiceImpl implements DictService {

    private static final String DICT_CACHE_PREFIX = "sys:dict:";
    private static final long DICT_CACHE_EXPIRE = 7 * 24 * 60 * 60; // 7天

    private final SysDictMapper sysDictMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private RedisService redisService;

    public DictServiceImpl(SysDictMapper sysDictMapper, 
                          SysDictDataMapper sysDictDataMapper) {
        this.sysDictMapper = sysDictMapper;
        this.sysDictDataMapper = sysDictDataMapper;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
        // Redis 可用时，异步加载字典缓存
        if (this.redisService != null) {
            try {
                log.info("Redis服务可用，开始加载字典数据到缓存...");
                loadDictCache();
                log.info("字典数据加载完成");
            } catch (Exception e) {
                log.warn("加载字典缓存失败，将使用数据库查询: {}", e.getMessage());
            }
        } else {
            log.info("Redis服务不可用，字典数据将直接从数据库查询");
        }
    }

    @Override
    public PageResult<DictDTO> getDictPage(Integer current, Integer size, String dictName, String dictType, Integer status) {
        Page<SysDict> page = new Page<>(current, size);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dictName), SysDict::getDictName, dictName)
                .like(StringUtils.hasText(dictType), SysDict::getDictType, dictType)
                .eq(status != null, SysDict::getStatus, status)
                .orderByAsc(SysDict::getDictType);

        Page<SysDict> dictPage = sysDictMapper.selectPage(page, wrapper);
        List<DictDTO> dictDTOList = dictPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dictDTOList, dictPage.getTotal(), dictPage.getCurrent(), dictPage.getSize());
    }

    @Override
    public DictDTO getDictById(Long id) {
        SysDict dict = sysDictMapper.selectById(id);
        return convertToDTO(dict);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDict(DictDTO dictDTO) {
        SysDict dict = new SysDict();
        BeanUtils.copyProperties(dictDTO, dict);
        sysDictMapper.insert(dict);
        
        // 刷新缓存
        refreshCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(Long id, DictDTO dictDTO) {
        SysDict dict = new SysDict();
        BeanUtils.copyProperties(dictDTO, dict);
        dict.setId(id);
        sysDictMapper.updateById(dict);
        
        // 刷新缓存
        refreshCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(Long id) {
        sysDictMapper.deleteById(id);
        
        // 删除该字典下的所有数据
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        SysDict dict = sysDictMapper.selectById(id);
        if (dict != null) {
            wrapper.eq(SysDictData::getDictType, dict.getDictType());
            sysDictDataMapper.delete(wrapper);
        }
        
        // 刷新缓存
        refreshCache();
    }

    @Override
    public List<DictDataDTO> getDictDataByType(String dictType) {
        // 先从缓存获取（如果Redis可用）
        if (redisService != null) {
            String cacheKey = DICT_CACHE_PREFIX + dictType;
            List<DictDataDTO> cachedData = (List<DictDataDTO>) redisService.get(cacheKey);
            if (cachedData != null) {
                return cachedData;
            }
        }

        // 缓存未命中或Redis不可用，从数据库查询
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictSort);
        
        List<SysDictData> dataList = sysDictDataMapper.selectList(wrapper);
        List<DictDataDTO> result = dataList.stream()
                .map(this::convertToDataDTO)
                .collect(Collectors.toList());

        // 存入缓存（如果Redis可用）
        if (redisService != null) {
            String cacheKey = DICT_CACHE_PREFIX + dictType;
            redisService.set(cacheKey, result, DICT_CACHE_EXPIRE);
        }
        
        return result;
    }

    @Override
    public PageResult<DictDataDTO> getDictDataPage(Integer current, Integer size, String dictType, String dictLabel, Integer status) {
        Page<SysDictData> page = new Page<>(current, size);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictType), SysDictData::getDictType, dictType)
                .like(StringUtils.hasText(dictLabel), SysDictData::getDictLabel, dictLabel)
                .eq(status != null, SysDictData::getStatus, status)
                .orderByAsc(SysDictData::getDictSort);

        Page<SysDictData> dataPage = sysDictDataMapper.selectPage(page, wrapper);
        List<DictDataDTO> dataDTOList = dataPage.getRecords().stream()
                .map(this::convertToDataDTO)
                .collect(Collectors.toList());

        return PageResult.of(dataDTOList, dataPage.getTotal(), dataPage.getCurrent(), dataPage.getSize());
    }

    @Override
    public DictDataDTO getDictDataById(Long id) {
        SysDictData dictData = sysDictDataMapper.selectById(id);
        return convertToDataDTO(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictData(DictDataDTO dictDataDTO) {
        SysDictData dictData = new SysDictData();
        BeanUtils.copyProperties(dictDataDTO, dictData);
        sysDictDataMapper.insert(dictData);
        
        // 刷新该字典类型的缓存
        refreshDictTypeCache(dictDataDTO.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(Long id, DictDataDTO dictDataDTO) {
        SysDictData dictData = new SysDictData();
        BeanUtils.copyProperties(dictDataDTO, dictData);
        dictData.setId(id);
        sysDictDataMapper.updateById(dictData);
        
        // 刷新该字典类型的缓存
        refreshDictTypeCache(dictDataDTO.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(Long id) {
        SysDictData dictData = sysDictDataMapper.selectById(id);
        if (dictData != null) {
            sysDictDataMapper.deleteById(id);
            // 刷新该字典类型的缓存
            refreshDictTypeCache(dictData.getDictType());
        }
    }

    @Override
    public void refreshCache() {
        log.info("刷新字典缓存...");
        loadDictCache();
    }

    @Override
    public void loadDictCache() {
        // 查询所有字典类型
        List<SysDict> dictList = sysDictMapper.selectList(
                new LambdaQueryWrapper<SysDict>().eq(SysDict::getStatus, 1)
        );

        // 为每个字典类型加载数据到缓存
        for (SysDict dict : dictList) {
            refreshDictTypeCache(dict.getDictType());
        }
        
        log.info("已加载 {} 个字典类型到缓存", dictList.size());
    }

    /**
     * 刷新指定字典类型的缓存
     */
    private void refreshDictTypeCache(String dictType) {
        if (redisService == null) {
            log.debug("Redis服务不可用，跳过缓存刷新");
            return;
        }
        
        String cacheKey = DICT_CACHE_PREFIX + dictType;
        
        // 查询该类型的所有字典数据
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictSort);
        
        List<SysDictData> dataList = sysDictDataMapper.selectList(wrapper);
        List<DictDataDTO> result = dataList.stream()
                .map(this::convertToDataDTO)
                .collect(Collectors.toList());

        // 更新缓存
        redisService.set(cacheKey, result, DICT_CACHE_EXPIRE);
        log.debug("已刷新字典类型 {} 的缓存，共 {} 条数据", dictType, result.size());
    }

    private DictDTO convertToDTO(SysDict dict) {
        if (dict == null) {
            return null;
        }
        DictDTO dto = new DictDTO();
        BeanUtils.copyProperties(dict, dto);
        return dto;
    }

    private DictDataDTO convertToDataDTO(SysDictData dictData) {
        if (dictData == null) {
            return null;
        }
        DictDataDTO dto = new DictDataDTO();
        BeanUtils.copyProperties(dictData, dto);
        return dto;
    }
}