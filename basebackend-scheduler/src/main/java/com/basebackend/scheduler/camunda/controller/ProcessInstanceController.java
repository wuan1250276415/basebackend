package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDeleteRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstancePageQuery;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceVariablesRequest;
import com.basebackend.scheduler.camunda.dto.ProcessVariableDTO;
import com.basebackend.scheduler.camunda.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Camunda 流程实例控制器
 *
 * <p>提供流程实例全生命周期管理能力，包括：
 * <ul>
 *   <li>流程实例分页查询与详情查看</li>
 *   <li>实例挂起、激活、删除等运行时操作</li>
 *   <li>流程变量的获取、设置、删除管理</li>
 *   <li>流程实例迁移与版本升级</li>
 *   <li>历史流程实例分页查询</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>RESTful API 设计，遵循标准 HTTP 方法语义</li>
 *   <li>完善的参数验证和错误处理机制</li>
 *   <li>支持租户隔离和多租户场景</li>
 *   <li>集成缓存机制提升查询性能</li>
 *   <li>详细的审计日志记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/process-instances")
@RequiredArgsConstructor
@Tag(name = "Camunda 流程实例管理", description = "流程实例运行、变量、迁移与历史查询 API")
@SecurityRequirement(name = "BearerAuth")
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    /**
     * 查看流程实例详情
     *
     * @param instanceId 流程实例 ID
     * @param withVariables 是否返回流程变量
     * @return 流程实例详情
     */
    @Operation(
        summary = "查看流程实例详情",
        description = "根据实例 ID 获取详细信息，可选择是否返回流程变量"
    )
    @GetMapping("/{instanceId}")
    public Result<ProcessInstanceDetailDTO> detail(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Parameter(description = "是否返回流程变量") @RequestParam(name = "withVariables", defaultValue = "false") boolean withVariables) {
        ProcessInstanceDetailDTO dto = processInstanceService.detail(instanceId, withVariables);
        return Result.success(dto);
    }

    /**
     * 挂起流程实例
     *
     * <p>挂起后流程实例将停止执行，无法继续推进。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @Operation(
        summary = "挂起流程实例",
        description = "挂起运行中的流程实例及其任务，阻止继续执行"
    )
    @PostMapping("/{instanceId}/suspend")
    public Result<String> suspend(@Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId) {
        processInstanceService.suspend(instanceId);
        return Result.success("流程实例挂起成功");
    }

    /**
     * 激活流程实例
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @Operation(
        summary = "激活流程实例",
        description = "恢复已挂起的流程实例，允许继续执行"
    )
    @PostMapping("/{instanceId}/activate")
    public Result<String> activate(@Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId) {
        processInstanceService.activate(instanceId);
        return Result.success("流程实例激活成功");
    }

    /**
     * 删除流程实例
     *
     * <p>支持跳过自定义监听器和 IO 映射，并标记为外部终止。
     * <p>为提高兼容性，参数可通过 Query Parameters 传递，避免 DELETE 请求携带 Body 的兼容性问题。
     * <p>同时支持 RequestBody 方式以保持向后兼容（可选）。Query Parameters 优先级高于 Body。
     *
     * @param instanceId 流程实例 ID
     * @param deleteReason 删除原因（可选）
     * @param skipCustomListeners 是否跳过自定义监听器（可选，默认 true）
     * @param skipIoMappings 是否跳过 IO 映射（可选，默认 true）
     * @param externallyTerminated 是否标记为外部终止（可选，默认 true）
     * @param request 删除请求参数（可选，用于向后兼容）
     * @return 操作结果
     */
    @Operation(
        summary = "删除流程实例",
        description = "删除指定的流程实例，支持跳过自定义监听器和 IO 映射。" +
                "参数可通过 Query Parameters 传递以避免 DELETE Body 兼容性问题，" +
                "同时支持 RequestBody 方式（可选）以保持向后兼容"
    )
    @DeleteMapping("/{instanceId}")
    public Result<String> delete(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Parameter(description = "删除原因，未提供时使用默认值")
            @RequestParam(value = "deleteReason", required = false) String deleteReason,
            @Parameter(description = "是否跳过自定义监听器，默认 true")
            @RequestParam(value = "skipCustomListeners", required = false) Boolean skipCustomListeners,
            @Parameter(description = "是否跳过 IO 映射，默认 true")
            @RequestParam(value = "skipIoMappings", required = false) Boolean skipIoMappings,
            @Parameter(description = "是否标记为外部终止，默认 true")
            @RequestParam(value = "externallyTerminated", required = false) Boolean externallyTerminated,
            @Parameter(description = "删除请求参数（可选，用于向后兼容）")
            @Valid @RequestBody(required = false) ProcessInstanceDeleteRequest request) {

        // DELETE 请求默认不携带 Body，优先使用 Query Parameters
        // 同时兼容旧版本客户端使用 Body 方式调用
        ProcessInstanceDeleteRequest effectiveRequest =
                request != null ? request : new ProcessInstanceDeleteRequest();

        // Query Parameters 优先级高于 Body
        if (deleteReason != null) {
            effectiveRequest.setDeleteReason(deleteReason);
        }
        if (skipCustomListeners != null) {
            effectiveRequest.setSkipCustomListeners(skipCustomListeners);
        }
        if (skipIoMappings != null) {
            effectiveRequest.setSkipIoMappings(skipIoMappings);
        }
        if (externallyTerminated != null) {
            effectiveRequest.setExternallyTerminated(externallyTerminated);
        }

        processInstanceService.delete(instanceId, effectiveRequest);
        return Result.success("流程实例删除成功");
    }

    /**
     * 查询流程实例的所有变量
     *
     * @param instanceId 流程实例 ID
     * @param local 是否只获取本地变量
     * @return 流程变量列表
     */
    @Operation(
        summary = "查询流程变量",
        description = "返回流程实例的全部流程变量，包含变量名、类型和值"
    )
    @GetMapping("/{instanceId}/variables")
    public Result<List<ProcessVariableDTO>> variables(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Parameter(description = "是否只获取本地变量") @RequestParam(name = "local", defaultValue = "false") boolean local) {
        List<ProcessVariableDTO> variables = processInstanceService.variables(instanceId, local);
        return Result.success(variables);
    }

    /**
     * 获取单个流程变量
     *
     * @param instanceId 流程实例 ID
     * @param variableName 变量名
     * @param local 是否获取本地变量
     * @return 流程变量
     */
    @Operation(
        summary = "获取单个流程变量",
        description = "根据变量名查询指定流程变量"
    )
    @GetMapping("/{instanceId}/variables/{variableName}")
    public Result<ProcessVariableDTO> variable(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Parameter(description = "变量名") @PathVariable @NotBlank String variableName,
            @Parameter(description = "是否获取本地变量") @RequestParam(name = "local", defaultValue = "false") boolean local) {
        ProcessVariableDTO variable = processInstanceService.variable(instanceId, variableName, local);
        return Result.success(variable);
    }

    /**
     * 批量设置流程变量
     *
     * <p>支持设置全局变量或本地变量。
     *
     * @param instanceId 流程实例 ID
     * @param request 变量设置请求
     * @return 操作结果
     */
    @Operation(
        summary = "设置流程变量",
        description = "批量设置流程变量，支持本地变量设置"
    )
    @PutMapping("/{instanceId}/variables")
    public Result<String> setVariables(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Valid @RequestBody ProcessInstanceVariablesRequest request) {
        processInstanceService.setVariables(instanceId, request);
        return Result.success("变量设置成功");
    }

    /**
     * 删除流程变量
     *
     * @param instanceId 流程实例 ID
     * @param variableName 变量名
     * @param local 是否删除本地变量
     * @return 操作结果
     */
    @Operation(
        summary = "删除流程变量",
        description = "删除指定的流程变量，支持本地变量删除"
    )
    @DeleteMapping("/{instanceId}/variables/{variableName}")
    public Result<String> deleteVariable(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Parameter(description = "变量名") @PathVariable @NotBlank String variableName,
            @Parameter(description = "是否删除本地变量") @RequestParam(name = "local", defaultValue = "false") boolean local) {
        processInstanceService.deleteVariable(instanceId, variableName, local);
        return Result.success("变量删除成功");
    }

    /**
     * 流程实例迁移/升级
     *
     * <p>将流程实例迁移到目标流程定义，支持活动映射和跳过监听器。
     *
     * @param instanceId 流程实例 ID
     * @param request 迁移请求
     * @return 操作结果
     */
    @Operation(
        summary = "迁移流程实例",
        description = "将流程实例迁移到目标流程定义，支持活动映射与跳过监听器"
    )
    @PostMapping("/{instanceId}/migrate")
    public Result<String> migrate(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @Valid @RequestBody ProcessInstanceMigrationRequest request) {
        processInstanceService.migrate(instanceId, request);
        return Result.success("流程实例迁移完成");
    }
}
