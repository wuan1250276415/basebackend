package com.basebackend.system.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 部门DTO
 */
@Data
public class DeptDTO {

    /**
     * 部门ID
     */
    private Long id;

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 30, message = "部门名称长度不能超过30个字符")
    private String deptName;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 负责人
     */
    @Size(max = 20, message = "负责人长度不能超过20个字符")
    private String leader;

    /**
     * 联系电话
     */
    @Size(max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    /**
     * 邮箱
     */
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /**
     * 部门状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /**
     * 子部门列表
     */
    private List<DeptDTO> children;
}
