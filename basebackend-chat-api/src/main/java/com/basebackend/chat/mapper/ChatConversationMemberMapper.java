package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConversationMemberMapper extends BaseMapper<ChatConversationMember> {
}
