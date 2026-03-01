package com.basebackend.api.model.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单审批请求 - 用于服务间通信
 */
@Data
public class TicketApprovalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "审批动作不能为空")
    private String action;

    private String opinion;

    private Long delegateToId;

    private String delegateToName;
}
