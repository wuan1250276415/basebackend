package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {
}
