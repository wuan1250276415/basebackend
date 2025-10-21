package com.basebackend.admin.service.storage.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.admin.entity.storage.SysBackupRecord;
import com.basebackend.admin.mapper.storage.SysBackupRecordMapper;
import com.basebackend.admin.service.storage.SysBackupService;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.service.MySQLBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 备份管理服务实现
 *
 * @author BaseBackend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysBackupServiceImpl extends ServiceImpl<SysBackupRecordMapper, SysBackupRecord> implements SysBackupService {

    private final MySQLBackupService mysqlBackupService;

    @Override
    public Long triggerFullBackup() {
        log.info("手动触发全量备份");

        // 调用备份服务
        BackupRecord backupRecord = mysqlBackupService.fullBackup();

        // 保存到数据库
        SysBackupRecord sysBackupRecord = convertToSysBackupRecord(backupRecord);
        save(sysBackupRecord);

        return sysBackupRecord.getId();
    }

    @Override
    public Long triggerIncrementalBackup() {
        log.info("手动触发增量备份");

        // 调用备份服务
        BackupRecord backupRecord = mysqlBackupService.incrementalBackup();

        // 保存到数据库
        SysBackupRecord sysBackupRecord = convertToSysBackupRecord(backupRecord);
        save(sysBackupRecord);

        return sysBackupRecord.getId();
    }

    @Override
    public boolean restoreDatabase(Long backupId) {
        log.info("恢复数据库，备份ID: {}", backupId);

        SysBackupRecord record = getById(backupId);
        if (record == null) {
            log.error("备份记录不存在");
            return false;
        }

        // 调用恢复服务
        return mysqlBackupService.restore(record.getBackupCode());
    }

    @Override
    public List<SysBackupRecord> listBackups(String backupType, String status) {
        LambdaQueryWrapper<SysBackupRecord> wrapper = new LambdaQueryWrapper<>();

        if (backupType != null && !backupType.isEmpty()) {
            wrapper.eq(SysBackupRecord::getBackupType, backupType);
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysBackupRecord::getStatus, status);
        }

        wrapper.orderByDesc(SysBackupRecord::getCreateTime);

        return list(wrapper);
    }

    @Override
    public boolean deleteBackup(Long backupId) {
        log.info("删除备份，ID: {}", backupId);

        SysBackupRecord record = getById(backupId);
        if (record == null) {
            return false;
        }

        // 调用删除服务
        boolean success = mysqlBackupService.deleteBackup(record.getBackupCode());

        if (success) {
            // 删除数据库记录
            removeById(backupId);
        }

        return success;
    }

    @Override
    public int cleanExpiredBackups() {
        log.info("清理过期备份");

        // 调用清理服务
        int count = mysqlBackupService.cleanExpiredBackups();

        // 同步更新数据库记录
        // TODO: 可以添加逻辑标记已删除的备份记录

        return count;
    }

    /**
     * 转换备份记录
     */
    private SysBackupRecord convertToSysBackupRecord(BackupRecord backupRecord) {
        SysBackupRecord sysBackupRecord = new SysBackupRecord();
        BeanUtils.copyProperties(backupRecord, sysBackupRecord);

        sysBackupRecord.setBackupCode(backupRecord.getBackupId());
        sysBackupRecord.setBackupType(backupRecord.getBackupType().getCode());
        sysBackupRecord.setStatus(backupRecord.getStatus().getCode());
        sysBackupRecord.setCreateTime(LocalDateTime.now());

        return sysBackupRecord;
    }
}
