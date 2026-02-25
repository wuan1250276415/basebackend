package com.basebackend.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典DTO
 */
public record DictDTO(
        /** 字典ID */
        Long id,

        /** 所属应用ID */
        Long appId,

        /** 字典名称 */
        @NotBlank(message = "字典名称不能为空")
        @Size(max = 100, message = "字典名称长度不能超过100个字符")
        String dictName,

        /** 字典类型 */
        @NotBlank(message = "字典类型不能为空")
        @Size(max = 100, message = "字典类型长度不能超过100个字符")
        String dictType,

        /** 状态：0-禁用，1-启用 */
        Integer status,

        /** 备注 */
        @Size(max = 500, message = "备注长度不能超过500个字符")
        String remark
) {
}
