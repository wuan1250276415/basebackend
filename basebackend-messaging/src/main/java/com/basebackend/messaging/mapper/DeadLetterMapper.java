package com.basebackend.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.messaging.entity.DeadLetterEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeadLetterMapper extends BaseMapper<DeadLetterEntity> {
}
