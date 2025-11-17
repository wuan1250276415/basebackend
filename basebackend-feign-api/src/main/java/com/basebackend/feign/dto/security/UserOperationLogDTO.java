package com.basebackend.feign.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作日志DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@Schema(name = "UserOperationLog", description = "用户操作日志")
public class UserOperationLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "操作模块：user-用户管理，role-角色管理，menu-菜单管理")
    private String module;

    @Schema(description = "操作类型：create-新增，update-修改，delete-删除，query-查询")
    private String operationType;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "响应状态：success-成功，error-失败")
    private String status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "操作耗时（毫秒）")
    private Long duration;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
