package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.antivirus.AntivirusService;
import com.basebackend.file.config.FileProperties;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FileRecycleBinMapper;
import com.basebackend.file.security.FileSecurityValidator;
import com.basebackend.file.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileManagementService 访问控制测试")
class FileManagementServiceAccessTest {

    @Mock
    private StorageService storageService;

    @Mock
    private FileProperties fileProperties;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @Mock
    private FileRecycleBinMapper fileRecycleBinMapper;

    @Mock
    private FileOperationLogMapper fileOperationLogMapper;

    @Mock
    private FileSecurityValidator fileSecurityValidator;

    @Mock
    private AntivirusService antivirusService;

    @Mock
    private FilePermissionService filePermissionService;

    @Mock
    private FileVersionService fileVersionService;

    @Mock
    private FileShareService fileShareService;

    @Mock
    private FileTagService fileTagService;

    @Mock
    private FileRecycleBinService fileRecycleBinService;

    @InjectMocks
    private FileManagementService fileManagementService;

    @Test
    @DisplayName("文件详情应拒绝无读取权限的普通用户")
    void shouldRejectFileDetailWhenRegularUserHasNoReadPermission() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(99L);
        metadata.setIsDeleted(false);
        metadata.setIsPublic(false);

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);
        when(filePermissionService.hasPermission("file-001", 7L, FilePermissionService.PermissionType.READ))
                .thenReturn(false);

        assertThatThrownBy(() -> fileManagementService.getFileDetail("file-001", 7L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限查看该文件");

        verify(filePermissionService).hasPermission("file-001", 7L, FilePermissionService.PermissionType.READ);
    }

    @Test
    @DisplayName("管理员查看文件详情时不应走普通读权限校验")
    void shouldAllowAdminToGetFileDetailWithoutPermissionLookup() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(99L);
        metadata.setIsDeleted(false);
        metadata.setIsPublic(false);

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);

        FileMetadata result = fileManagementService.getFileDetail("file-001", 7L, true);

        assertThat(result).isSameAs(metadata);
        verifyNoInteractions(filePermissionService);
    }

    @Test
    @DisplayName("普通用户具备读取权限时应返回文件详情")
    void shouldReturnFileDetailWhenRegularUserHasReadPermission() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOwnerId(99L);
        metadata.setIsDeleted(false);
        metadata.setIsPublic(false);

        when(fileMetadataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);
        when(filePermissionService.hasPermission("file-001", 7L, FilePermissionService.PermissionType.READ))
                .thenReturn(true);

        FileMetadata result = fileManagementService.getFileDetail("file-001", 7L, false);

        assertThat(result).isSameAs(metadata);
        verify(filePermissionService).hasPermission("file-001", 7L, FilePermissionService.PermissionType.READ);
    }

    @Test
    @DisplayName("普通用户查询文件列表时应附带可见范围过滤")
    void shouldApplyReadableScopeWhenListFilesForRegularUser() {
        Page<FileMetadata> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of());
        pageResult.setTotal(0);

        when(fileMetadataMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageResult);

        PageResult<FileMetadata> page = fileManagementService.listFiles(
                null, null, null, null, null, null, null,
                1, 10, 7L, false
        );

        ArgumentCaptor<QueryWrapper<FileMetadata>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileMetadataMapper).selectPage(any(Page.class), wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .contains("owner_id")
                .contains("is_public")
                .contains("file_permission")
                .contains("permission_type");
        assertThat(page.getRecords()).isEmpty();
    }

    @Test
    @DisplayName("管理员查询文件列表时不应附带普通可见范围过滤")
    void shouldNotApplyReadableScopeWhenListFilesForAdmin() {
        Page<FileMetadata> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of());
        pageResult.setTotal(0);

        when(fileMetadataMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageResult);

        fileManagementService.listFiles(
                null, null, null, null, null, null, null,
                1, 10, 7L, true
        );

        ArgumentCaptor<QueryWrapper<FileMetadata>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileMetadataMapper).selectPage(any(Page.class), wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .doesNotContain("file_permission")
                .doesNotContain("permission_type")
                .doesNotContain("is_public")
                .doesNotContain("owner_id");
    }
}
