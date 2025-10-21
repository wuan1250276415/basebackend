package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 配置回滚DTO
 */
@Data
public class ConfigRollbackDTO {

    /**
     * 配置ID
     */
    @NotNull(message = "配置ID不能为空")
    private Long configId;

    /**
     * 回滚到的历史版本ID
     */
    @NotNull(message = "历史版本ID不能为空")
    private Long historyId;
}
