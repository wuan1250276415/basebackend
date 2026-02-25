package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;

/**
 * 配置回滚DTO
 */
public record ConfigRollbackDTO(
    /** 配置ID */
    @NotNull(message = "配置ID不能为空") Long configId,
    /** 回滚到的历史版本ID */
    @NotNull(message = "历史版本ID不能为空") Long historyId
) {}
