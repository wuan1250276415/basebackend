package com.basebackend.admin.dto.notification;

/**
 * 通知列表查询 DTO
 *
 * @author Claude Code
 * @since 2025-11-07
 */
public record NotificationQueryDTO(
    /** 页码 */
    Integer page,
    /** 每页大小 */
    Integer pageSize,
    /** 通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知, all-全部 */
    String type,
    /** 通知级别：info-信息, warning-警告, error-错误, success-成功, all-全部 */
    String level,
    /** 是否已读：0-未读, 1-已读, all-全部 */
    String isRead,
    /** 关键词搜索（标题或内容） */
    String keyword
) {
    public NotificationQueryDTO {
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 10;
    }
}
