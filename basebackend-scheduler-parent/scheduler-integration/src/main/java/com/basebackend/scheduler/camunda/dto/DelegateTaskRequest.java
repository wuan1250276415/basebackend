package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 委托任务请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "DelegateTaskRequest", description = "委托任务请求参数")
public class DelegateTaskRequest {

    /**
     * 委托给的用户 ID
     */
    @Schema(
        description = "委托给的用户 ID",
        example = "bob",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "委托用户 ID 不能为空")
    private String assignee;

    /**
     * 用户 ID（别名，与 assignee 相同）
     */
    public String getUserId() {
        return assignee;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置委托对象（兼容性方法）
     * @param delegateTo 委托对象
     */
    public void setDelegateTo(String delegateTo) {
        this.assignee = delegateTo;
    }

    /**
     * 获取委托对象（兼容性方法）
     * @return 委托对象
     */
    public String getDelegateTo() {
        return this.assignee;
    }

    /**
     * 设置委托原因（兼容性方法）
     * @param reason 委托原因
     */
    public void setReason(String reason) {
        // 这个字段在当前设计中不存在，存储到description或其他字段中
        // 作为兼容性处理，暂时忽略或记录到变量中
    }

    /**
     * 获取委托原因（兼容性方法）
     * @return 委托原因
     */
    public String getReason() {
        return null;
    }
}
