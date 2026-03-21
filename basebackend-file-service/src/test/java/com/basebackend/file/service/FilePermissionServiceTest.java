package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FilePermission;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FilePermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilePermissionService 权限判断测试")
class FilePermissionServiceTest {

    @Mock
    private FilePermissionMapper filePermissionMapper;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @Mock
    private FileOperationLogMapper fileOperationLogMapper;

    @InjectMocks
    private FilePermissionService filePermissionService;

    @Test
    @DisplayName("文件所有者应拥有所有权限且无需查询显式授权")
    void shouldGrantAllPermissionsToOwnerWithoutPermissionLookup() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(7L);
        metadata.setIsPublic(false);

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);

        boolean allowed = filePermissionService.hasPermission(
                "file-001", 7L, FilePermissionService.PermissionType.DELETE);

        assertThat(allowed).isTrue();
        verifyNoInteractions(filePermissionMapper);
    }

    @Test
    @DisplayName("公开文件应允许非所有者直接读取")
    void shouldAllowReadForPublicFileWithoutExplicitPermission() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(9L);
        metadata.setIsPublic(true);

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);

        boolean allowed = filePermissionService.hasPermission(
                "file-001", 7L, FilePermissionService.PermissionType.READ);

        assertThat(allowed).isTrue();
        verifyNoInteractions(filePermissionMapper);
    }

    @Test
    @DisplayName("显式授权存在时应允许访问")
    void shouldAllowAccessWhenExplicitPermissionExists() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(9L);
        metadata.setIsPublic(false);

        FilePermission permission = new FilePermission();
        permission.setFileId("file-001");
        permission.setUserId(7L);
        permission.setPermissionType(FilePermissionService.PermissionType.WRITE.name());

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);
        when(filePermissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permission);

        boolean allowed = filePermissionService.hasPermission(
                "file-001", 7L, FilePermissionService.PermissionType.WRITE);

        assertThat(allowed).isTrue();
        verify(filePermissionMapper).selectOne(any(LambdaQueryWrapper.class));
    }
}
