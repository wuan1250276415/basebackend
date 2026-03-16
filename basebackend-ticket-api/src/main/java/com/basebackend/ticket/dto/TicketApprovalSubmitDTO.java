package com.basebackend.ticket.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 工单审批提交请求
 */
public record TicketApprovalSubmitDTO(
        @NotBlank(message = "一级审批人不能为空")
        String approver1,

        String approver2
) implements Serializable {
}
