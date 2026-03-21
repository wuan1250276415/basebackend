package com.basebackend.album;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.basebackend.album.controller.AlbumController;
import com.basebackend.album.controller.PhotoController;
import com.basebackend.album.controller.ShareController;
import com.basebackend.album.mapper.AlbumMapper;
import com.basebackend.album.mapper.PhotoMapper;
import com.basebackend.album.service.AlbumService;
import com.basebackend.album.service.PhotoService;
import com.basebackend.album.service.ShareService;
import com.basebackend.album.service.impl.AlbumServiceImpl;
import com.basebackend.album.service.impl.PhotoServiceImpl;
import com.basebackend.album.service.impl.ShareServiceImpl;
import org.junit.jupiter.api.Test;

class AlbumApiStructureSmokeTest {

    @Test
    void coreAlbumClassesShouldBeReachable() {
        assertAll(
            () -> assertNotNull(AlbumController.class),
            () -> assertNotNull(PhotoController.class),
            () -> assertNotNull(ShareController.class),
            () -> assertNotNull(AlbumService.class),
            () -> assertNotNull(PhotoService.class),
            () -> assertNotNull(ShareService.class),
            () -> assertNotNull(AlbumServiceImpl.class),
            () -> assertNotNull(PhotoServiceImpl.class),
            () -> assertNotNull(ShareServiceImpl.class),
            () -> assertNotNull(AlbumMapper.class),
            () -> assertNotNull(PhotoMapper.class)
        );
    }
}
