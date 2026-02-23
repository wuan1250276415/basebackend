package com.basebackend.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.messaging.entity.WebhookEndpointEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WebhookEndpointMapper extends BaseMapper<WebhookEndpointEntity> {

    List<WebhookEndpointEntity> selectSubscribed(@org.apache.ibatis.annotations.Param("eventType") String eventType);
}
