package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatMessageRead;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageReadMapper extends BaseMapper<ChatMessageRead> {
}
