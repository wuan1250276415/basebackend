package com.basebackend.album;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AlbumApiApplicationSmokeTest {

    @Test
    void applicationClassShouldBePresent() {
        assertNotNull(AlbumApiApplication.class);
    }
}
