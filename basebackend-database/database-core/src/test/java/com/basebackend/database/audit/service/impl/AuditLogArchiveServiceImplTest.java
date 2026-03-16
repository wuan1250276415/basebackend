package com.basebackend.database.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.entity.AuditLogArchive;
import com.basebackend.database.audit.mapper.AuditLogArchiveMapper;
import com.basebackend.database.audit.mapper.AuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogArchiveServiceImpl 测试")
class AuditLogArchiveServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private AuditLogArchiveMapper auditLogArchiveMapper;

    @InjectMocks
    private AuditLogArchiveServiceImpl auditLogArchiveService;

    @Test
    @DisplayName("仅删除已成功归档的日志")
    void shouldDeleteOnlySuccessfullyArchivedLogs() {
        AuditLog first = createAuditLog(1L);
        AuditLog second = createAuditLog(2L);

        when(auditLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));
        when(auditLogArchiveMapper.insert(any(AuditLogArchive.class)))
                .thenReturn(1)
                .thenThrow(new RuntimeException("mock insert failure"));
        when(auditLogMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        int archivedCount = auditLogArchiveService.archiveExpiredLogs(30);

        assertThat(archivedCount).isEqualTo(1);

        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> selectCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> deleteCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectList(selectCaptor.capture());
        verify(auditLogMapper).delete(deleteCaptor.capture());
        verify(auditLogArchiveMapper, times(2)).insert(any(AuditLogArchive.class));

        assertThat(deleteCaptor.getValue()).isNotSameAs(selectCaptor.getValue());
    }

    @Test
    @DisplayName("全部归档失败时不执行删除")
    void shouldNotDeleteWhenArchiveFailed() {
        AuditLog onlyLog = createAuditLog(3L);

        when(auditLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(onlyLog));
        when(auditLogArchiveMapper.insert(any(AuditLogArchive.class)))
                .thenThrow(new RuntimeException("archive failed"));

        int archivedCount = auditLogArchiveService.archiveExpiredLogs(30);

        assertThat(archivedCount).isZero();
        verify(auditLogMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    private AuditLog createAuditLog(Long id) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setOperateTime(Date.from(LocalDateTime.now().minusDays(60)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        return log;
    }
}
