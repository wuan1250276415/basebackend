package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典数据Mapper接口
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {


    /**
     * 根据字典类型查询字典数据列表
     */
    List<SysDictData> selectDictDataByType(@Param("dictType") String dictType);

    /**
     * 根据字典类型和字典键值查询字典数据
     */
    SysDictData selectDictDataByTypeAndValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue);
}
