package com.basebackend.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.messaging.entity.MessageLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLogEntity> {

    List<MessageLogEntity> selectTimeoutMessages(@Param("statuses") List<String> statuses,
                                                  @Param("timeoutMinutes") long timeoutMinutes,
                                                  @Param("limit") int limit);
}
