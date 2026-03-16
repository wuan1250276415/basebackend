package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatMessageForward;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageForwardMapper extends BaseMapper<ChatMessageForward> {
}
