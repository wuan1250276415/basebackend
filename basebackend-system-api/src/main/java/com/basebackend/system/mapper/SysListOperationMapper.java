package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.SysListOperation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 列表操作定义Mapper接口
 */
@Mapper
public interface SysListOperationMapper extends BaseMapper<SysListOperation> {
}
