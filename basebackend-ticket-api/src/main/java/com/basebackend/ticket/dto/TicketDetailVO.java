package com.basebackend.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情展示对象
 */
@Data
public class TicketDetailVO {

    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String status;
    private Integer priority;
    private Long categoryId;
    private String categoryName;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private Long deptId;
    private LocalDateTime slaDeadline;
    private Integer slaBreached;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String processInstanceId;
    private Integer commentCount;
    private Integer attachmentCount;
    private String tags;
    private String source;
    private String extraData;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<CommentItem> comments;
    private List<AttachmentItem> attachments;
    private List<StatusLogItem> statusLogs;
    private List<ApprovalItem> approvals;
    private List<CcItem> ccList;

    /**
     * 评论条目
     */
    public record CommentItem(
            Long id,
            String content,
            String type,
            Integer isInternal,
            Long parentId,
            String creatorName,
            LocalDateTime createTime
    ) {
    }

    /**
     * 附件条目
     */
    public record AttachmentItem(
            Long id,
            Long fileId,
            String fileName,
            Long fileSize,
            String fileType,
            String fileUrl,
            LocalDateTime createTime
    ) {
    }

    /**
     * 状态变更日志条目
     */
    public record StatusLogItem(
            Long id,
            String fromStatus,
            String toStatus,
            String operatorName,
            String remark,
            LocalDateTime createTime
    ) {
    }

    /**
     * 审批记录条目
     */
    public record ApprovalItem(
            Long id,
            String taskName,
            String approverName,
            String action,
            String opinion,
            String delegateToName,
            LocalDateTime createTime
    ) {
    }

    /**
     * 抄送条目
     */
    public record CcItem(
            Long id,
            String userName,
            Integer isRead,
            LocalDateTime readTime,
            LocalDateTime createTime
    ) {
    }
}
