package com.basebackend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 列表操作定义实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_list_operation")
public class SysListOperation extends BaseEntity {

    /**
     * 操作编码（唯一标识）
     */
    @TableField("operation_code")
    private String operationCode;

    /**
     * 操作名称
     */
    @TableField("operation_name")
    private String operationName;

    /**
     * 操作类型：view-查看，add-新增，edit-编辑，delete-删除，export-导出，import-导入
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 适用资源类型（NULL表示通用）
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 操作图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
