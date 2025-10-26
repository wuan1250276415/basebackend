package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_template")
public class GenTemplate extends BaseEntity {

    /**
     * 分组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 模板编码
     */
    @TableField("code")
    private String code;

    /**
     * 模板内容
     */
    @TableField("template_content")
    private String templateContent;

    /**
     * 输出路径模板
     */
    @TableField("output_path")
    private String outputPath;

    /**
     * 文件后缀
     */
    @TableField("file_suffix")
    private String fileSuffix;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否内置
     */
    @TableField("is_builtin")
    private Integer isBuiltin;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
