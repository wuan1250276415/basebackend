package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 任务附件添加请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "AttachmentRequest", description = "任务附件添加请求参数")
public class AttachmentRequest {

    /**
     * 附件名称
     */
    @Schema(
        description = "附件名称",
        example = "订单文档.pdf",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "附件名称不能为空")
    private String name;

    /**
     * 附件描述
     */
    @Schema(
        description = "附件描述",
        example = "订单相关文档"
    )
    private String description;

    /**
     * 附件类型
     */
    @Schema(
        description = "附件类型",
        example = "document",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "附件类型不能为空")
    private String type;

    /**
     * 附件 URL（外部链接）
     */
    @Schema(
        description = "附件 URL（外部链接）",
        example = "https://storage.example.com/documents/order.pdf",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "附件 URL 不能为空")
    private String url;

    /**
     * 流程实例 ID
     */
    @Schema(
        description = "流程实例 ID（可选）",
        example = "12345"
    )
    private String processInstanceId;

    /**
     * 附件内容（字节数组）
     */
    @Schema(
        description = "附件内容（字节数组）",
        hidden = true
    )
    private byte[] content;

    // 手动添加 getter/setter 方法以确保编译成功
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
