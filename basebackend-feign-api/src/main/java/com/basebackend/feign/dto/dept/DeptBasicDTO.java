package com.basebackend.feign.dto.dept;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门基础信息 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "部门基础信息")
public class DeptBasicDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门负责人ID")
    private Long leaderId;

    @Schema(description = "部门负责人姓名")
    private String leaderName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "部门邮箱")
    private String email;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "显示顺序（兼容orderNum字段）")
    private Integer orderNum;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子部门列表")
    private List<DeptBasicDTO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
