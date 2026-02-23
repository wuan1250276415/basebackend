package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.entity.FileTag;
import com.basebackend.file.entity.FileTagRelation;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FileTagMapper;
import com.basebackend.file.mapper.FileTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件标签服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTagService {

    private final FileTagMapper fileTagMapper;
    private final FileTagRelationMapper fileTagRelationMapper;
    private final FileOperationLogMapper fileOperationLogMapper;

    /**
     * 获取所有标签
     */
    public List<FileTag> getAllTags() {
        return fileTagMapper.selectList(
            new LambdaQueryWrapper<FileTag>()
                .orderByAsc(FileTag::getTagName)
        );
    }

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public FileTag createTag(FileTag tag, Long userId, String userName) {
        tag.setId(null);
        tag.setCreatedBy(userId);
        tag.setCreatedByName(userName);
        tag.setUsageCount(0);
        fileTagMapper.insert(tag);
        return tag;
    }

    /**
     * 为文件添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFileTag(String fileId, Long tagId, Long userId, String userName) {
        FileTagRelation existing = fileTagRelationMapper.selectOne(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        FileTagRelation relation = new FileTagRelation();
        relation.setFileId(fileId);
        relation.setTagId(tagId);
        relation.setCreatedBy(userId);
        relation.setCreatedByName(userName);
        fileTagRelationMapper.insert(relation);

        FileTag tag = fileTagMapper.selectById(tagId);
        if (tag != null) {
            tag.setUsageCount((tag.getUsageCount() == null ? 0 : tag.getUsageCount()) + 1);
            fileTagMapper.updateById(tag);
        }

        logOperation(fileId, "ADD_TAG", userId, userName, "添加标签: " + tagId);
    }

    /**
     * 为文件移除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFileTag(String fileId, Long tagId, Long userId) {
        fileTagRelationMapper.delete(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
        );

        FileTag tag = fileTagMapper.selectById(tagId);
        if (tag != null && tag.getUsageCount() != null && tag.getUsageCount() > 0) {
            tag.setUsageCount(tag.getUsageCount() - 1);
            fileTagMapper.updateById(tag);
        }

        logOperation(fileId, "REMOVE_TAG", userId, null, "移除标签: " + tagId);
    }

    /**
     * 获取文件标签
     */
    public List<FileTag> getFileTags(String fileId) {
        List<FileTagRelation> relations = fileTagRelationMapper.selectList(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
        );
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        List<Long> tagIds = relations.stream()
            .map(FileTagRelation::getTagId)
            .collect(Collectors.toList());
        return fileTagMapper.selectBatchIds(tagIds);
    }

    private void logOperation(String fileId, String operationType, Long operatorId,
                               String operatorName, String detail) {
        FileOperationLog operationLog = new FileOperationLog();
        operationLog.setFileId(fileId);
        operationLog.setOperationType(operationType);
        operationLog.setOperatorId(operatorId);
        operationLog.setOperatorName(operatorName);
        operationLog.setOperationDetail(detail);
        operationLog.setOperationTime(LocalDateTime.now());
        fileOperationLogMapper.insert(operationLog);
    }
}
