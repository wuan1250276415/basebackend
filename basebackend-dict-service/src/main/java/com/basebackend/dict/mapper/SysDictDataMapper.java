package com.basebackend.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.dict.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据Mapper接口
 *
 * @author BaseBackend Team
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<SysDictData> selectDictDataByType(@Param("dictType") String dictType);

    /**
     * 根据字典类型和字典键值查询字典数据
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典数据对象
     */
    SysDictData selectDictDataByTypeAndValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue);
}
