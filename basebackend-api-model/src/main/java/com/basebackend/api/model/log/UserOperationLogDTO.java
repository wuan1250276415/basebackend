package com.basebackend.api.model.log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户操作日志 DTO（跨服务通信用）
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "用户操作日志")
public record UserOperationLogDTO(

        @Schema(description = "日志ID") Long id,

        @Schema(description = "用户ID") Long userId,

        @Schema(description = "操作类型（login, logout, update_profile, change_password等）") String operationType,

        @Schema(description = "操作描述") String operationDesc,

        @Schema(description = "IP地址") String ipAddress,

        @Schema(description = "位置") String location,

        @Schema(description = "浏览器") String browser,

        @Schema(description = "操作系统") String os,

        @Schema(description = "请求参数") String requestParams,

        @Schema(description = "操作状态（0-失败，1-成功）") Integer status,

        @Schema(description = "错误信息") String errorMsg,

        @Schema(description = "创建时间") LocalDateTime createTime

) implements Serializable {
}
