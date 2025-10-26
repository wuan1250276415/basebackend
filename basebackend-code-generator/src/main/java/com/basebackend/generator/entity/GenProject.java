package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_project")
public class GenProject extends BaseEntity {

    /**
     * 项目名称
     */
    @TableField("name")
    private String name;

    /**
     * 包名
     */
    @TableField("package_name")
    private String packageName;

    /**
     * 作者
     */
    @TableField("author")
    private String author;

    /**
     * 版本
     */
    @TableField("version")
    private String version;

    /**
     * 基础路径
     */
    @TableField("base_path")
    private String basePath;

    /**
     * 模块名
     */
    @TableField("module_name")
    private String moduleName;

    /**
     * 表前缀
     */
    @TableField("table_prefix")
    private String tablePrefix;

    /**
     * 模板分组ID
     */
    @TableField("template_group_id")
    private Long templateGroupId;

    /**
     * 其他配置JSON
     */
    @TableField("config_json")
    private String configJson;
}
