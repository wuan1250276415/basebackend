package com.basebackend.logging.audit.dsar;

import com.basebackend.logging.audit.export.AuditExportService;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GDPR 数据主体访问请求 (DSAR) 服务
 *
 * 支持数据主体根据 GDPR 第 15 条（访问权）、第 17 条（删除权）行使权利。
 * 提供按 userId 查询、导出和匿名化审计日志的能力。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class DsarService {

    private static final String ANONYMIZED = "[ANONYMIZED]";
    private static final int DEFAULT_QUERY_LIMIT = 10000;

    private final AuditStorage storage;
    private final AuditExportService exportService;

    public DsarService(AuditStorage storage, AuditExportService exportService) {
        this.storage = storage;
        this.exportService = exportService;
    }

    /**
     * GDPR Article 15 — 数据主体访问：查询指定用户的所有审计记录
     *
     * @param userId 数据主体用户 ID
     * @param limit  最大返回条数
     * @return 该用户的审计日志列表
     */
    public List<AuditLogEntry> accessRequest(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<AuditLogEntry> entries = storage.findByUserId(userId, limit > 0 ? limit : DEFAULT_QUERY_LIMIT);
            log.info("DSAR 访问请求完成: userId={}, 返回 {} 条记录", userId, entries.size());
            return entries;
        } catch (Exception e) {
            log.error("DSAR 访问请求失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * GDPR Article 15 — 导出指定用户的审计记录为指定格式
     *
     * @param userId 数据主体用户 ID
     * @param format 导出格式 (csv, cef, ocsf, leef)
     * @return 导出内容字符串
     */
    public String exportUserData(String userId, String format) {
        List<AuditLogEntry> entries = accessRequest(userId, DEFAULT_QUERY_LIMIT);
        if (entries.isEmpty()) {
            return "";
        }
        return switch (format.toLowerCase()) {
            case "csv" -> exportService.exportToCsv(entries);
            case "cef" -> exportService.exportToCef(entries);
            case "ocsf" -> exportService.exportToOcsf(entries);
            case "leef" -> exportService.exportToLeef(entries);
            default -> exportService.exportToCsv(entries);
        };
    }

    /**
     * GDPR Article 17 — 匿名化指定用户的审计记录
     *
     * 审计日志不可物理删除（合规要求保留操作记录），
     * 因此采用匿名化策略：将个人身份信息替换为 [ANONYMIZED]。
     *
     * <p><b>注意</b>：单条 save 失败时记录错误日志并继续处理其余条目，
     * 最终返回成功匿名化的条目数。调用方应根据返回值与预期总数对比判断是否全部成功。
     * 如需事务性保证，宿主服务应在 @Transactional 方法中包装本调用。
     *
     * @param userId 数据主体用户 ID
     * @return 匿名化成功处理的记录数（小于总记录数时表示有部分失败）
     */
    public int anonymizeUserData(String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        int count = 0;
        int failed = 0;
        try {
            List<AuditLogEntry> entries = storage.findByUserId(userId, DEFAULT_QUERY_LIMIT);
            for (AuditLogEntry entry : entries) {
                try {
                    anonymizeEntry(entry);
                    storage.save(entry);
                    count++;
                } catch (Exception e) {
                    failed++;
                    log.error("DSAR 匿名化单条记录失败: id={}, userId={}", entry.getId(), userId, e);
                }
            }
            if (failed > 0) {
                log.warn("DSAR 匿名化部分失败: userId={}, 成功={}, 失败={}", userId, count, failed);
            } else {
                log.info("DSAR 匿名化完成: userId={}, 处理 {} 条记录", userId, count);
            }
            return count;
        } catch (Exception e) {
            log.error("DSAR 匿名化失败: userId={}", userId, e);
            return count;
        }
    }

    /**
     * 获取指定用户的审计数据摘要（不含详细内容）
     */
    public Map<String, Object> getUserDataSummary(String userId) {
        List<AuditLogEntry> entries = accessRequest(userId, DEFAULT_QUERY_LIMIT);
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalRecords", entries.size());
        if (!entries.isEmpty()) {
            summary.put("earliestRecord", entries.get(entries.size() - 1).getTimestamp());
            summary.put("latestRecord", entries.get(0).getTimestamp());
        }
        return summary;
    }

    private void anonymizeEntry(AuditLogEntry entry) {
        entry.setUserId(ANONYMIZED);
        entry.setSessionId(ANONYMIZED);
        entry.setClientIp(ANONYMIZED);
        entry.setUserAgent(ANONYMIZED);
        entry.setDeviceInfo(ANONYMIZED);
        entry.setLocation(ANONYMIZED);
        if (entry.getDetails() != null) {
            entry.getDetails().replaceAll((k, v) -> ANONYMIZED);
        }
    }
}
