package com.basebackend.chat.dto.request;

import lombok.Data;

/**
 * 修改群信息请求
 */
@Data
public class UpdateGroupRequest {

    private String name;
    private String avatar;
    private String description;

    /** 入群方式: 0-自由加入 1-需审批 2-仅邀请 */
    private Integer joinMode;

    /** 邀请需确认: 0-直接入群 1-被邀请人确认 */
    private Boolean inviteConfirm;
}
