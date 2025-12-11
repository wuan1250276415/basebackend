package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.task.Attachment;

import java.util.Date;

/**
 * 任务附件数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AttachmentDTO", description = "任务附件信息")
public class AttachmentDTO {

    /**
     * 附件 ID
     */
    @Schema(description = "附件 ID", example = "att_12345")
    private String id;

    /**
     * 附件名称
     */
    @Schema(description = "附件名称", example = "订单文档.pdf")
    private String name;

    /**
     * 附件描述
     */
    @Schema(description = "附件描述", example = "订单相关文档")
    private String description;

    /**
     * 附件类型
     */
    @Schema(description = "附件类型", example = "document")
    private String type;

    /**
     * 附件 URL
     */
    @Schema(description = "附件 URL", example = "https://storage.example.com/documents/order.pdf")
    private String url;

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
     * 创建人
     */
    @Schema(description = "创建人", example = "alice")
    private String userId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-01-01 10:00:00")
    private Date time;

    /**
     * 设置创建时间（兼容性方法）
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.time = createTime;
    }

    /**
     * 从 Camunda Attachment 转换为 DTO
     *
     * @param attachment Camunda 附件
     * @return DTO 对象
     */
    public static AttachmentDTO from(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return AttachmentDTO.builder()
                .id(attachment.getId())
                .name(attachment.getName())
                .description(attachment.getDescription())
                .type(attachment.getType())
                .url(attachment.getUrl())
                .taskId(attachment.getTaskId())
                .processInstanceId(attachment.getProcessInstanceId())
                .build();
    }
}
