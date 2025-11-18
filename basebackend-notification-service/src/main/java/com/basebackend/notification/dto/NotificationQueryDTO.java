package com.basebackend.notification.dto;

import lombok.Data;

/**
 * 通知列表查询 DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
public class NotificationQueryDTO {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知, all-全部
     */
    private String type;

    /**
     * 通知级别：info-信息, warning-警告, error-错误, success-成功, all-全部
     */
    private String level;

    /**
     * 是否已读：0-未读, 1-已读, all-全部
     */
    private String isRead;

    /**
     * 关键词搜索（标题或内容）
     */
    private String keyword;
}
