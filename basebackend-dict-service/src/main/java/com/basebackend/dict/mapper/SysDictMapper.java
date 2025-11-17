package com.basebackend.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.dict.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据字典Mapper接口
 *
 * @author BaseBackend Team
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 根据字典类型查询字典
     *
     * @param dictType 字典类型
     * @return 字典对象
     */
    SysDict selectByDictType(@Param("dictType") String dictType);

    /**
     * 检查字典类型是否唯一
     *
     * @param dictType 字典类型
     * @param dictId   字典ID（更新时需要排除自己）
     * @return 存在的数量
     */
    int checkDictTypeUnique(@Param("dictType") String dictType, @Param("dictId") Long dictId);
}
