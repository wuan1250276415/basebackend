package com.basebackend.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.generator.entity.GenHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生成历史Mapper
 */
@Mapper
public interface GenHistoryMapper extends BaseMapper<GenHistory> {
}
