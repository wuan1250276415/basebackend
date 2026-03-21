package com.basebackend.chat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.basebackend.chat.controller.ChatConversationController;
import com.basebackend.chat.controller.ChatFriendController;
import com.basebackend.chat.controller.ChatGroupController;
import com.basebackend.chat.controller.ChatMessageController;
import com.basebackend.chat.mapper.ChatConversationMapper;
import com.basebackend.chat.mapper.ChatFriendMapper;
import com.basebackend.chat.mapper.ChatGroupMapper;
import com.basebackend.chat.mapper.ChatMessageMapper;
import com.basebackend.chat.service.ChatConversationService;
import com.basebackend.chat.service.ChatFriendService;
import com.basebackend.chat.service.ChatGroupService;
import com.basebackend.chat.service.ChatMessageService;
import com.basebackend.chat.service.impl.ChatConversationServiceImpl;
import com.basebackend.chat.service.impl.ChatFriendServiceImpl;
import com.basebackend.chat.service.impl.ChatGroupServiceImpl;
import com.basebackend.chat.service.impl.ChatMessageServiceImpl;
import org.junit.jupiter.api.Test;

class ChatApiStructureSmokeTest {

    @Test
    void coreChatClassesShouldBeReachable() {
        assertAll(
            () -> assertNotNull(ChatConversationController.class),
            () -> assertNotNull(ChatFriendController.class),
            () -> assertNotNull(ChatGroupController.class),
            () -> assertNotNull(ChatMessageController.class),
            () -> assertNotNull(ChatConversationService.class),
            () -> assertNotNull(ChatFriendService.class),
            () -> assertNotNull(ChatGroupService.class),
            () -> assertNotNull(ChatMessageService.class),
            () -> assertNotNull(ChatConversationServiceImpl.class),
            () -> assertNotNull(ChatFriendServiceImpl.class),
            () -> assertNotNull(ChatGroupServiceImpl.class),
            () -> assertNotNull(ChatMessageServiceImpl.class),
            () -> assertNotNull(ChatConversationMapper.class),
            () -> assertNotNull(ChatFriendMapper.class),
            () -> assertNotNull(ChatGroupMapper.class),
            () -> assertNotNull(ChatMessageMapper.class)
        );
    }
}
