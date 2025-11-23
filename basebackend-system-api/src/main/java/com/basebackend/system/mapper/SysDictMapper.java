package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.database.dynamic.annotation.DS;
import com.basebackend.system.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 字典Mapper接口
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 根据字典类型查询
     */
    @Select("SELECT * FROM sys_dict WHERE dict_type = #{dictType} AND deleted = 0 LIMIT 1")
    SysDict selectByDictType(String dictType);
}
