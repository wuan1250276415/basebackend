package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    /**
     * 字典名称
     */
    @TableField("dict_name")
    private String dictName;

    /**
     * 字典类型
     */
    @TableField("dict_type")
    private String dictType;

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
