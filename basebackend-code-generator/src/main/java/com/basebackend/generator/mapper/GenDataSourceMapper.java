package com.basebackend.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.generator.entity.GenDataSource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置Mapper
 */
@Mapper
public interface GenDataSourceMapper extends BaseMapper<GenDataSource> {
}
