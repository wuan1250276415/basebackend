package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板分组实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_template_group")
public class GenTemplateGroup extends BaseEntity {

    /**
     * 分组名称
     */
    @TableField("name")
    private String name;

    /**
     * 分组编码
     */
    @TableField("code")
    private String code;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 模板引擎类型
     */
    @TableField("engine_type")
    private String engineType;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
