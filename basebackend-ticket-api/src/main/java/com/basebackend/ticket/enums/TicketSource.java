package com.basebackend.ticket.enums;

/**
 * 工单来源枚举
 */
public enum TicketSource {

    WEB("Web端"),
    API("API接口"),
    EMAIL("邮件"),
    WECHAT("微信");

    private final String description;

    TicketSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
