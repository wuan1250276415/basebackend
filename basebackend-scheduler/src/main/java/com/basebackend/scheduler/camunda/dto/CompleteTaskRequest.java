package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 完成任务请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "CompleteTaskRequest", description = "完成任务请求参数")
public class CompleteTaskRequest {

    /**
     * 输出变量集合
     */
    @Schema(
        description = "输出变量集合，传递给流程的下一步",
        example = "{\"approved\": true, \"comment\": \"同意审批\"}"
    )
    private Map<String, Object> variables = new HashMap<>();

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置评论（兼容性方法）
     * @param comment 评论
     */
    public void setComment(String comment) {
        this.variables.put("comment", comment);
    }

    /**
     * 获取评论（兼容性方法）
     * @return 评论
     */
    public String getComment() {
        return (String) this.variables.get("comment");
    }

    /**
     * 设置审批结果（兼容性方法）
     * @param approved 审批结果
     */
    public void setApproved(Boolean approved) {
        this.variables.put("approved", approved);
    }

    /**
     * 获取审批结果（兼容性方法）
     * @return 审批结果
     */
    public Boolean getApproved() {
        return (Boolean) this.variables.get("approved");
    }
}
