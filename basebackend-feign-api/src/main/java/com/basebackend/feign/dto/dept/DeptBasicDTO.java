package com.basebackend.feign.dto.dept;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门基础信息 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "部门基础信息")
public record DeptBasicDTO(

        @Schema(description = "部门ID")
        Long id,

        @Schema(description = "父部门ID")
        Long parentId,

        @Schema(description = "部门名称")
        String deptName,

        @Schema(description = "部门编码")
        String deptCode,

        @Schema(description = "部门负责人ID")
        Long leaderId,

        @Schema(description = "部门负责人姓名")
        String leaderName,

        @Schema(description = "联系电话")
        String phone,

        @Schema(description = "部门邮箱")
        String email,

        @Schema(description = "排序号")
        Integer sort,

        @Schema(description = "显示顺序（兼容orderNum字段）")
        Integer orderNum,

        @Schema(description = "状态：0-禁用，1-启用")
        Integer status,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "子部门列表")
        List<DeptBasicDTO> children,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime

) implements Serializable {
}
