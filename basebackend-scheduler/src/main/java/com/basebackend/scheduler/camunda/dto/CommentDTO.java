package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.task.Comment;

import java.util.Date;

/**
 * 任务评论数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CommentDTO", description = "任务评论信息")
public class CommentDTO {

    /**
     * 评论 ID
     */
    @Schema(description = "评论 ID", example = "cmt_12345")
    private String id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "alice")
    private String userId;

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID", example = "12345")
    private String taskId;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "proc_12345")
    private String processInstanceId;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容", example = "已完成审批，建议通过")
    private String message;

    /**
     * 评论时间
     */
    @Schema(description = "评论时间", example = "2025-01-01 10:00:00")
    private Date time;

    /**
     * 从 Camunda Comment 转换为 DTO
     *
     * @param comment Camunda 评论对象
     * @return DTO 对象
     */
    public static CommentDTO from(Comment comment) {
        if (comment == null) {
            return null;
        }
        return CommentDTO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .taskId(comment.getTaskId())
                .processInstanceId(comment.getProcessInstanceId())
                .message(comment.getFullMessage())
                .time(comment.getTime())
                .build();
    }
}
