package com.basebackend.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 应用资源DTO
 */
@Data
public class ApplicationResourceDTO {

    /**
     * 资源ID
     */
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 父资源ID
     */
    private Long parentId;

    /**
     * 资源类型：M-目录，C-菜单，F-按钮
     */
    private String resourceType;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 图标
     */
    private String icon;

    /**
     * 是否可见：0-隐藏，1-显示
     */
    private Integer visible;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 子资源列表
     */
    private List<ApplicationResourceDTO> children;
}
