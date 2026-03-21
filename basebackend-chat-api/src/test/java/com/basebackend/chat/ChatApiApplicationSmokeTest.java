package com.basebackend.chat;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ChatApiApplicationSmokeTest {

    @Test
    void applicationClassShouldBePresent() {
        assertNotNull(ChatApiApplication.class);
    }
}
