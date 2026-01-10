package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.BinaryPayload;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDTO;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDeployRequest;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionPageQuery;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStateRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.service.ProcessDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Camunda 流程定义控制器
 *
 * <p>
 * 提供完整的 Camunda 流程定义生命周期管理功能，包括：
 * <ul>
 * <li>流程定义部署（支持 BPMN XML 和 ZIP 文件）</li>
 * <li>流程定义查询（支持分页、租户、最新版本过滤）</li>
 * <li>流程定义详情查看</li>
 * <li>流程部署删除（支持级联删除）</li>
 * <li>BPMN XML 和流程图下载</li>
 * <li>流程实例启动</li>
 * <li>流程定义挂起和激活</li>
 * </ul>
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li>RESTful API 设计，遵循标准 HTTP 方法语义</li>
 * <li>完善的参数验证和错误处理机制</li>
 * <li>支持租户隔离和多租户场景</li>
 * <li>集成缓存机制提升查询性能</li>
 * <li>详细的审计日志记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/process-definitions")
@RequiredArgsConstructor
@Tag(name = "Camunda 流程定义管理", description = "Camunda 流程定义生命周期管理 API")
@SecurityRequirement(name = "BearerAuth")
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    /**
     * 部署流程定义
     *
     * <p>
     * 上传 BPMN 文件并部署到 Camunda 引擎。支持：
     * <ul>
     * <li>BPMN 2.0 XML 文件</li>
     * <li>多租户部署</li>
     * <li>重复过滤（避免重复部署相同流程）</li>
     * </ul>
     *
     * @param request 部署请求参数
     * @return 部署 ID
     */
    @Operation(summary = "部署流程定义", description = "上传 BPMN 文件并部署到 Camunda 引擎")
    @PostMapping(value = "/deployments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> deploy(@Valid @ModelAttribute ProcessDefinitionDeployRequest request) {
        String deploymentId = processDefinitionService.deploy(request);
        return Result.success("流程定义部署成功", deploymentId);
    }

    /**
     * 部署流程定义（已废弃）
     *
     * <p>
     * 为保持向后兼容而保留的遗留端点。
     * 请使用 {@code POST /api/camunda/process-definitions/deployments} 代替。
     *
     * @param request 部署请求参数
     * @return 部署 ID
     * @deprecated 请改用 {@link #deploy(ProcessDefinitionDeployRequest)}，使用 POST
     *             /deployments 路径
     */
    @Deprecated
    @Operation(summary = "部署流程定义（已废弃）", description = "遗留部署端点，已废弃。请改用 POST /deployments", deprecated = true)
    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> deployLegacy(@Valid @ModelAttribute ProcessDefinitionDeployRequest request) {
        return deploy(request);
    }

    /**
     * 分页查询流程定义
     *
     * <p>
     * 支持多种过滤条件：
     * <ul>
     * <li>流程定义 Key 模糊搜索</li>
     * <li>流程定义名称模糊搜索</li>
     * <li>租户 ID 过滤</li>
     * <li>只查询最新版本</li>
     * <li>挂起状态过滤</li>
     * </ul>
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询流程定义", description = "支持 Key、名称、租户、最新版本、挂起状态过滤")
    @GetMapping
    public Result<PageResult<ProcessDefinitionDTO>> page(@ParameterObject @Valid ProcessDefinitionPageQuery query) {
        PageResult<ProcessDefinitionDTO> result = processDefinitionService.page(query);
        return Result.success(result);
    }

    /**
     * 获取流程定义详情
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    @Operation(summary = "获取流程定义详情", description = "根据流程定义 ID 获取详细信息，包括部署信息")
    @GetMapping("/{definitionId}")
    public Result<ProcessDefinitionDetailDTO> detail(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId) {
        ProcessDefinitionDetailDTO dto = processDefinitionService.detail(definitionId);
        return Result.success(dto);
    }

    /**
     * 删除流程部署
     *
     * @param deploymentId 部署 ID
     * @param cascade      是否级联删除关联的流程实例
     * @return 删除结果
     */
    @Operation(summary = "删除流程部署", description = "删除指定的流程部署，可选择是否级联删除关联的流程实例")
    @DeleteMapping("/deployments/{deploymentId}")
    public Result<String> deleteDeployment(
            @Parameter(description = "部署 ID") @PathVariable @NotBlank String deploymentId,
            @Parameter(description = "是否级联删除流程实例") @RequestParam(value = "cascade", defaultValue = "false") boolean cascade) {
        processDefinitionService.deleteDeployment(deploymentId, cascade);
        return Result.success("流程部署删除成功");
    }

    /**
     * 下载 BPMN XML 文件
     *
     * @param definitionId 流程定义 ID
     * @return BPMN XML 文件
     */
    @Operation(summary = "下载 BPMN XML", description = "下载指定流程定义的 BPMN XML 文件")
    @GetMapping("/{definitionId}/xml")
    public ResponseEntity<Resource> downloadBpmn(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId) {
        BinaryPayload payload = processDefinitionService.downloadBpmn(definitionId);

        ByteArrayResource resource = new ByteArrayResource(payload.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.getFileName() + "\"")
                .body(resource);
    }

    /**
     * 获取 BPMN XML 内容（用于前端预览）
     *
     * @param definitionId 流程定义 ID
     * @return BPMN XML 内容字符串
     */
    @Operation(summary = "获取 BPMN XML 内容", description = "获取指定流程定义的 BPMN XML 内容字符串，用于前端预览")
    @GetMapping("/{definitionId}/xml-content")
    public Result<java.util.Map<String, String>> getBpmnXmlContent(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId) {
        BinaryPayload payload = processDefinitionService.downloadBpmn(definitionId);
        String xml = new String(payload.getData(), java.nio.charset.StandardCharsets.UTF_8);
        return Result.success(java.util.Map.of("xml", xml));
    }

    /**
     * 下载流程图
     *
     * @param definitionId 流程定义 ID
     * @return 流程图文件（PNG 或 SVG）
     */
    @Operation(summary = "下载流程图", description = "下载指定流程定义的流程图（PNG 或 SVG 格式）")
    @GetMapping("/{definitionId}/diagram")
    public ResponseEntity<Resource> downloadDiagram(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId) {
        BinaryPayload payload = processDefinitionService.downloadDiagram(definitionId);

        ByteArrayResource resource = new ByteArrayResource(payload.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.getFileName() + "\"")
                .body(resource);
    }

    /**
     * 启动流程实例
     *
     * <p>
     * 根据流程定义 ID 或 Key 启动新的流程实例。
     * 如果只提供 Key，则自动使用最新版本的流程定义。
     *
     * @param request 启动请求参数
     * @return 流程实例信息
     */
    // 启动流程实例已迁移至 ProcessInstanceController
    /*
     * @Operation(summary = "启动流程实例", description = "根据流程定义 ID 或 Key 启动新的流程实例")
     * 
     * @PostMapping("/start")
     * public Result<ProcessInstanceDTO> startInstance(
     * 
     * @Valid @RequestBody ProcessDefinitionStartRequest request) {
     * ProcessInstanceDTO dto = processDefinitionService.startInstance(request);
     * return Result.success("流程实例启动成功", dto);
     * }
     */

    /**
     * 挂起流程定义
     *
     * @param definitionId 流程定义 ID
     * @param request      挂起请求参数
     * @return 操作结果
     */
    @Operation(summary = "挂起流程定义", description = "挂起指定的流程定义，可选择是否同时挂起已运行的流程实例")
    @PostMapping("/{definitionId}/suspend")
    public Result<String> suspend(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId,
            @Valid @RequestBody ProcessDefinitionStateRequest request) {
        processDefinitionService.suspend(definitionId, request.isIncludeProcessInstances());
        return Result.success("流程定义已挂起");
    }

    /**
     * 激活流程定义
     *
     * @param definitionId 流程定义 ID
     * @param request      激活请求参数
     * @return 操作结果
     */
    @Operation(summary = "激活流程定义", description = "激活指定的流程定义，可选择是否同时激活已运行的流程实例")
    @PostMapping("/{definitionId}/activate")
    public Result<String> activate(
            @Parameter(description = "流程定义 ID") @PathVariable @NotBlank String definitionId,
            @Valid @RequestBody ProcessDefinitionStateRequest request) {
        processDefinitionService.activate(definitionId, request.isIncludeProcessInstances());
        return Result.success("流程定义已激活");
    }
}
