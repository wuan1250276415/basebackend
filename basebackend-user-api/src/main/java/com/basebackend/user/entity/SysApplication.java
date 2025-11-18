package com.basebackend.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_application")
public class SysApplication extends BaseEntity {

    /**
     * 应用名称
     */
    @TableField("app_name")
    private String appName;

    /**
     * 应用编码（唯一标识）
     */
    @TableField("app_code")
    private String appCode;

    /**
     * 应用类型（web/mobile/api等）
     */
    @TableField("app_type")
    private String appType;

    /**
     * 应用图标
     */
    @TableField("app_icon")
    private String appIcon;

    /**
     * 应用地址
     */
    @TableField("app_url")
    private String appUrl;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
