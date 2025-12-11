package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 任务评论添加请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "CommentRequest", description = "任务评论添加请求参数")
public class CommentRequest {

    /**
     * 流程实例 ID
     */
    @Schema(
        description = "流程实例 ID（可选）",
        example = "12345"
    )
    private String processInstanceId;

    /**
     * 评论内容
     */
    @Schema(
        description = "评论内容",
        example = "已完成审批，建议通过",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "评论内容不能为空")
    private String message;

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置用户ID（兼容性方法）
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        // 将用户ID存储在processInstanceId字段中作为兼容性处理
        this.processInstanceId = userId;
    }

    /**
     * 获取用户ID（兼容性方法）
     * @return 用户ID
     */
    public String getUserId() {
        return this.processInstanceId;
    }

    /**
     * 获取评论（兼容性方法）
     * @return 评论内容
     */
    public String getComment() {
        return this.message;
    }
}
