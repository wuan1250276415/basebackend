package com.basebackend.admin.mapper.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.messaging.SysDeadLetter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 死信Mapper
 */
@Mapper
public interface SysDeadLetterMapper extends BaseMapper<SysDeadLetter> {
}
