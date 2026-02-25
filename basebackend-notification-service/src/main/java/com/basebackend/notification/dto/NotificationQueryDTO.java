package com.basebackend.notification.dto;

/**
 * 通知列表查询 DTO
 *
 * @param page     页码（默认1）
 * @param pageSize 每页大小（默认10）
 * @param type     通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知, all-全部
 * @param level    通知级别：info-信息, warning-警告, error-错误, success-成功, all-全部
 * @param isRead   是否已读：0-未读, 1-已读, all-全部
 * @param keyword  关键词搜索（标题或内容）
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public record NotificationQueryDTO(
        Integer page,
        Integer pageSize,
        String type,
        String level,
        String isRead,
        String keyword
) {
    /**
     * 提供默认值的紧凑构造器
     */
    public NotificationQueryDTO {
        if (page == null) {
            page = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
    }
}
