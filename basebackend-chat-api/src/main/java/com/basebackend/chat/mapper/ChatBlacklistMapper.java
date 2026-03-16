package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatBlacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatBlacklistMapper extends BaseMapper<ChatBlacklist> {
}
