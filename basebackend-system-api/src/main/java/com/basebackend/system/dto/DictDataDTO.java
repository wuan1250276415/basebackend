package com.basebackend.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典数据DTO
 */
public record DictDataDTO(
        /** 字典数据ID */
        Long id,

        /** 所属应用ID */
        Long appId,

        /** 字典排序 */
        Integer dictSort,

        /** 字典标签 */
        @NotBlank(message = "字典标签不能为空")
        @Size(max = 100, message = "字典标签长度不能超过100个字符")
        String dictLabel,

        /** 字典键值 */
        @NotBlank(message = "字典键值不能为空")
        @Size(max = 100, message = "字典键值长度不能超过100个字符")
        String dictValue,

        /** 字典类型 */
        @NotBlank(message = "字典类型不能为空")
        @Size(max = 100, message = "字典类型长度不能超过100个字符")
        String dictType,

        /** 样式属性 */
        @Size(max = 100, message = "样式属性长度不能超过100个字符")
        String cssClass,

        /** 表格回显样式 */
        @Size(max = 100, message = "表格回显样式长度不能超过100个字符")
        String listClass,

        /** 是否默认：0-否，1-是 */
        Integer isDefault,

        /** 状态：0-禁用，1-启用 */
        Integer status,

        /** 备注 */
        @Size(max = 500, message = "备注长度不能超过500个字符")
        String remark
) {
}
