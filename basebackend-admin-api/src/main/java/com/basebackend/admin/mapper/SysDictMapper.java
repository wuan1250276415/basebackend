package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据字典Mapper接口
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 根据字典类型查询字典
     */
    SysDict selectByDictType(@Param("dictType") String dictType);

    /**
     * 检查字典类型是否唯一
     */
    int checkDictTypeUnique(@Param("dictType") String dictType, @Param("dictId") Long dictId);
}
