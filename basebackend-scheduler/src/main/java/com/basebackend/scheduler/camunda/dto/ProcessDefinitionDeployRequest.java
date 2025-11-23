package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 流程定义部署请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessDefinitionDeployRequest", description = "流程定义部署请求")
public class ProcessDefinitionDeployRequest {

    /**
     * BPMN 文件（XML 或 ZIP）
     */
    @Schema(
        description = "BPMN 文件（XML 或 ZIP）",
        requiredMode = Schema.RequiredMode.REQUIRED,
        type = "string",
        format = "binary"
    )
    @NotNull(message = "BPMN 文件不能为空")
    private MultipartFile file;

    /**
     * 部署名称
     */
    @Schema(description = "部署名称，默认使用文件名")
    private String deploymentName;

    /**
     * 租户 ID
     */
    @Schema(description = "Camunda 租户 ID，支持多租户场景")
    private String tenantId;

    /**
     * 流程分类
     */
    @Schema(description = "流程定义分类")
    private String category;

    /**
     * 是否启用重复部署过滤
     */
    @Schema(
        description = "是否启用重复部署过滤，默认 true",
        defaultValue = "true"
    )
    private boolean enableDuplicateFilter = true;

    /**
     * 是否只部署变更的资源
     */
    @Schema(
        description = "是否只部署变更的资源，默认 false",
        defaultValue = "false"
    )
    private boolean deployChangedOnly = false;

    /**
     * 部署名称（别名）
     */
    @Schema(description = "部署名称")
    private String name;

    /**
     * 部署来源
     */
    @Schema(description = "部署来源")
    private String source;

    /**
     * BPMN 内容（字节数组）
     */
    @Schema(description = "BPMN 内容")
    private byte[] bpmnContent;

    /**
     * 资源名称
     */
    @Schema(description = "资源名称")
    private String resourceName;

    /**
     * 是否启用重复过滤（兼容方法）
     */
    public boolean isEnableDuplicateFiltering() {
        return enableDuplicateFilter;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置BPMN XML内容（兼容性方法）
     * @param bpmnXmlContent BPMN XML内容
     */
    public void setBpmnXmlContent(String bpmnXmlContent) {
        this.bpmnContent = bpmnXmlContent == null ? null : bpmnXmlContent.getBytes();
    }

    /**
     * 获取BPMN XML内容（兼容性方法）
     * @return BPMN XML内容
     */
    public String getBpmnXmlContent() {
        return this.bpmnContent == null ? null : new String(this.bpmnContent);
    }

    /**
     * 设置流程Key（兼容性方法）
     * @param processKey 流程Key
     */
    public void setProcessKey(String processKey) {
        this.name = processKey;
    }

    /**
     * 获取流程Key（兼容性方法）
     * @return 流程Key
     */
    public String getProcessKey() {
        return this.name;
    }

    /**
     * 设置流程名称（兼容性方法）
     * @param processName 流程名称
     */
    public void setProcessName(String processName) {
        this.deploymentName = processName;
    }

    /**
     * 获取流程名称（兼容性方法）
     * @return 流程名称
     */
    public String getProcessName() {
        return this.deploymentName;
    }
}
