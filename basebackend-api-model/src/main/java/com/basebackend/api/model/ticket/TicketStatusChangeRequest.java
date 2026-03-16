package com.basebackend.api.model.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单状态变更请求 - 用于服务间通信
 */
@Data
public class TicketStatusChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    private String remark;
}
