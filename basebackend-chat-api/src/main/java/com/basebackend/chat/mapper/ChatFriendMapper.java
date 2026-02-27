package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatFriend;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatFriendMapper extends BaseMapper<ChatFriend> {
}
