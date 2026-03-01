package com.basebackend.ticket.enums;

/**
 * 工单优先级枚举
 */
public enum TicketPriority {

    URGENT(1, "紧急"),
    HIGH(2, "高"),
    MEDIUM(3, "中"),
    LOW(4, "低");

    private final int code;
    private final String description;

    TicketPriority(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TicketPriority fromCode(int code) {
        for (TicketPriority p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        return MEDIUM;
    }
}
