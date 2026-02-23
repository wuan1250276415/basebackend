package com.basebackend.scheduler.form.model.data;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 表单数据
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class FormData {
    /**
     * 数据ID
     */
    private String id;
    
    /**
     * 表单模板ID
     */
    private String templateId;
    
    /**
     * 表单数据
     */
    private Map<String, Object> data;
    
    /**
     * 附件
     */
    private Map<String, Object> attachments;
    
    /**
     * 提交人
     */
    private String submittedBy;
    
    /**
     * 提交时间
     */
    private Instant submittedAt;
    
    /**
     * 最后修改人
     */
    private String lastModifiedBy;
    
    /**
     * 最后修改时间
     */
    private Instant lastModifiedAt;
    
    /**
     * 数据状态
     */
    private DataStatus status = DataStatus.DRAFT;
    
    /**
     * 审批状态
     */
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    /**
     * 审批历史
     */
    private java.util.List<ApprovalHistory> approvalHistory;
    
    /**
     * 关联的流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 关联的任务ID
     */
    private String taskId;
    
    /**
     * 业务键
     */
    private String businessKey;
    
    /**
     * 版本号
     */
    private int version = 1;
    
    /**
     * 是否已删除
     */
    private boolean deleted = false;
    
    /**
     * 删除时间
     */
    private Instant deletedAt;
    
    /**
     * 删除人
     */
    private String deletedBy;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    public enum DataStatus {
        DRAFT,        // 草稿
        SUBMITTED,    // 已提交
        APPROVED,     // 已批准
        REJECTED,     // 已拒绝
        ARCHIVED      // 已归档
    }
    
    public enum ApprovalStatus {
        PENDING,     // 待审批
        APPROVED,    // 已批准
        REJECTED,    // 已拒绝
        CANCELLED    // 已取消
    }
    
    @Data
    public static class ApprovalHistory {
        private String approver;
        private String action; // APPROVE, REJECT
        private String comment;
        private Instant timestamp;
        private String reason;
    }
}
