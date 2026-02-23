package com.basebackend.scheduler.form.version;

import com.basebackend.scheduler.form.model.schema.FormSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表单版本管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormVersionService {
    
    private final Map<String, List<FormVersion>> versions = new ConcurrentHashMap<>();
    
    public void createVersion(String templateId, FormSchema schema, String version, String comment, String createdBy) {
        log.debug("Creating version {} for template: {}", version, templateId);
        
        FormVersion formVersion = FormVersion.builder()
                .id(UUID.randomUUID().toString())
                .templateId(templateId)
                .version(version)
                .schema(copySchema(schema))
                .comment(comment)
                .createdBy(createdBy)
                .createdAt(Instant.now().toEpochMilli())
                .isActive(false)
                .build();
        
        versions.computeIfAbsent(templateId, k -> new ArrayList<>()).add(formVersion);
        
        // 如果是第一个版本，设为活跃
        List<FormVersion> versionList = versions.get(templateId);
        if (versionList.size() == 1) {
            activateVersion(templateId, version);
        }
    }
    
    public List<FormVersion> getVersions(String templateId) {
        return versions.getOrDefault(templateId, new ArrayList<>());
    }
    
    public Optional<FormVersion> getVersion(String templateId, String version) {
        List<FormVersion> versionList = versions.get(templateId);
        if (versionList == null) {
            return Optional.empty();
        }
        
        return versionList.stream()
                .filter(v -> v.getVersion().equals(version))
                .findFirst();
    }
    
    public Optional<FormVersion> getActiveVersion(String templateId) {
        List<FormVersion> versionList = versions.get(templateId);
        if (versionList == null) {
            return Optional.empty();
        }
        
        return versionList.stream()
                .filter(FormVersion::isActive)
                .findFirst();
    }
    
    public void activateVersion(String templateId, String version) {
        log.debug("Activating version {} for template: {}", version, templateId);
        
        List<FormVersion> versionList = versions.get(templateId);
        if (versionList == null) {
            throw new RuntimeException("No versions found for template: " + templateId);
        }
        
        // 取消所有活跃版本
        versionList.forEach(v -> v.setActive(false));
        
        // 激活指定版本
        versionList.stream()
                .filter(v -> v.getVersion().equals(version))
                .findFirst()
                .ifPresent(v -> v.setActive(true));
    }
    
    public void rollbackVersion(String templateId, String targetVersion) {
        log.info("Rolling back template {} to version {}", templateId, targetVersion);
        
        Optional<FormVersion> targetVersionOpt = getVersion(templateId, targetVersion);
        if (targetVersionOpt.isEmpty()) {
            throw new RuntimeException("Version not found: " + targetVersion);
        }
        
        activateVersion(templateId, targetVersion);
    }
    
    public void compareVersions(String templateId, String version1, String version2) {
        // 实际实现应比较两个版本的差异
        log.debug("Comparing versions {} and {} for template: {}", version1, version2, templateId);
    }
    
    private FormSchema copySchema(FormSchema schema) {
        // 实际实现应使用深拷贝
        // 这里使用简单实现
        return schema;
    }
}
