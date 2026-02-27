package com.basebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.chat.entity.ChatFriendRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatFriendRequestMapper extends BaseMapper<ChatFriendRequest> {
}
